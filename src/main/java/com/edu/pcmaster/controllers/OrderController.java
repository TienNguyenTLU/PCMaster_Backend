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
import org.springframework.web.bind.annotation.RestController;

import com.edu.pcmaster.dto.order.OrderItemResponse;
import com.edu.pcmaster.dto.order.OrderRequest;
import com.edu.pcmaster.dto.order.OrderResponse;
import com.edu.pcmaster.models.Order;
import com.edu.pcmaster.models.OrderStatus;
import com.edu.pcmaster.services.CurrentUserService;
import com.edu.pcmaster.services.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
	private final OrderService orderService;
	private final CurrentUserService currentUserService;

	public OrderController(OrderService orderService, CurrentUserService currentUserService) {
		this.orderService = orderService;
		this.currentUserService = currentUserService;
	}

	// ── Customer endpoints ──────────────────────────────────────────────────────

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

	// ── Admin endpoints ─────────────────────────────────────────────────────────

	@GetMapping("/admin/all")
	@PreAuthorize("hasRole('ADMIN')")
	public List<OrderResponse> adminListAll() {
		return orderService.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	@GetMapping("/admin/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public OrderResponse adminDetail(@PathVariable Long id) {
		return toResponse(orderService.getById(id));
	}

	@PutMapping("/admin/{id}/confirm")
	@PreAuthorize("hasRole('ADMIN')")
	public OrderResponse adminConfirm(@PathVariable Long id) {
		return toResponse(orderService.confirm(id));
	}

	@PutMapping("/admin/{id}/status")
	@PreAuthorize("hasRole('ADMIN')")
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

	// ── Mapper ─────────────────────────────────────────────────────────────────

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
