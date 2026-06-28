package com.edu.pcmaster.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import com.edu.pcmaster.dto.order.OrderItemResponse;
import com.edu.pcmaster.dto.order.OrderRequest;
import com.edu.pcmaster.dto.order.OrderResponse;
import com.edu.pcmaster.models.Order;
import com.edu.pcmaster.models.OrderStatus;
import com.edu.pcmaster.services.CurrentUserService;
import com.edu.pcmaster.services.OrderService;

import jakarta.validation.Valid;

import com.edu.pcmaster.services.VnpayService;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
	private final OrderService orderService;
	private final CurrentUserService currentUserService;
	private final VnpayService vnpayService;

	public OrderController(OrderService orderService, CurrentUserService currentUserService, VnpayService vnpayService) {
		this.orderService = orderService;
		this.currentUserService = currentUserService;
		this.vnpayService = vnpayService;
	}

	

	@PostMapping
	public OrderResponse create(@Valid @RequestBody OrderRequest request) {
		return toResponse(orderService.create(request, currentUserService.requireUser()));
	}

	@GetMapping
	public List<OrderResponse> list() {
		return orderService.findByUser(currentUserService.requireUser()).stream()
				.map(this::toResponse)
				.toList();
	}

	@GetMapping("/{id}")
	public OrderResponse detail(@PathVariable Long id) {
		return toResponse(orderService.getByIdForUser(id, currentUserService.requireUser()));
	}

	

	@GetMapping("/admin/all")
	@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
	public List<OrderResponse> adminListAll() {
		return orderService.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	@GetMapping("/admin/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
	public OrderResponse adminDetail(@PathVariable Long id) {
		return toResponse(orderService.getById(id));
	}

	@PutMapping("/admin/{id}/confirm")
	@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
	public OrderResponse adminConfirm(@PathVariable Long id) {
		return toResponse(orderService.confirm(id));
	}

	@PutMapping("/admin/{id}/reject")
	@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
	public OrderResponse adminReject(@PathVariable Long id, @RequestBody Map<String, String> body) {
		String reason = body.get("reason");
		if (reason == null || reason.isBlank()) {
			throw new com.edu.pcmaster.common.exception.BadRequestException("Vui lòng nhập lý do từ chối");
		}
		return toResponse(orderService.reject(id, reason));
	}

	@PutMapping("/admin/{id}/status")
	@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
	public ResponseEntity<OrderResponse> adminUpdateStatus(
			@PathVariable Long id,
			@RequestBody Map<String, String> body) {
		String statusStr = body.get("status");
		if (statusStr == null) {
			return ResponseEntity.badRequest().build();
		}
		OrderStatus newStatus;
		try {
			newStatus = OrderStatus.valueOf(statusStr);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
		return ResponseEntity.ok(toResponse(orderService.updateStatus(id, newStatus)));
	}

	@GetMapping("/{id}/payment-url")
	public ResponseEntity<Map<String, String>> getPaymentUrl(@PathVariable Long id, HttpServletRequest httpRequest) {
		Order order = orderService.getByIdForUser(id, currentUserService.requireUser());
		if (order.getPaymentMethod() != com.edu.pcmaster.models.PaymentMethod.VNPAY) {
			throw new com.edu.pcmaster.common.exception.BadRequestException("Order payment method is not VNPAY");
		}
		String ip = httpRequest.getHeader("X-FORWARDED-FOR");
		if (ip == null) {
			ip = httpRequest.getRemoteAddr();
		}
		String paymentUrl = vnpayService.generatePaymentUrl(order, ip);
		return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
	}

	@GetMapping("/vnpay-ipn")
	public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> params) {
		if (!vnpayService.verifyCallback(params)) {
			return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid Checksum"));
		}

		Long orderId;
		try {
			orderId = Long.parseLong(params.get("vnp_TxnRef"));
		} catch (NumberFormatException e) {
			return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order not found"));
		}

		Order order = orderService.getById(orderId);
		if (order == null) {
			return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order not found"));
		}

		java.math.BigDecimal vnpAmount = new java.math.BigDecimal(params.get("vnp_Amount"))
				.divide(new java.math.BigDecimal(100));
		if (order.getTotalAmount().compareTo(vnpAmount) != 0) {
			return ResponseEntity.ok(Map.of("RspCode", "04", "Message", "Invalid Amount"));
		}

		if (order.getPaymentStatus() == com.edu.pcmaster.models.PaymentStatus.PAID) {
			return ResponseEntity.ok(Map.of("RspCode", "02", "Message", "Order already confirmed"));
		}

		String responseCode = params.get("vnp_ResponseCode");
		if ("00".equals(responseCode)) {
			order.setPaymentStatus(com.edu.pcmaster.models.PaymentStatus.PAID);
			orderService.save(order);
		} else {
			order.setPaymentStatus(com.edu.pcmaster.models.PaymentStatus.FAILED);
			order.setStatus(com.edu.pcmaster.models.OrderStatus.CANCELLED);
			orderService.save(order);
		}

		return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
	}

	

	private OrderResponse toResponse(Order order) {
		String username = order.getUser() != null ? order.getUser().getUsername() : null;
		String email = order.getUser() != null ? order.getUser().getEmail() : null;

		return new OrderResponse(
				order.getId(),
				order.getUser() == null ? null : order.getUser().getId(),
				username,
				email,
				order.getTotalAmount(),
				order.getStatus(),
				order.getDeliveryType(),
				order.getRecipientName(),
				order.getRecipientPhone(),
				order.getShippingAddress(),
				order.getDocumentUrl(),
				order.getCreatedAt(),
				order.getCoupon() != null ? order.getCoupon().getCode() : null,
				order.getCouponDiscount(),
				order.getPaymentMethod(),
				order.getPaymentStatus(),
				order.getAppointmentTime(),
				order.getRejectReason(),
				order.getItems().stream()
						.map(item -> new OrderItemResponse(
								item.getId(),
								item.getProduct() == null ? null : item.getProduct().getId(),
								item.getQuantity(),
								item.getSellingPrice(),
								item.getCostPrice()
						))
						.toList()
		);
	}
}
