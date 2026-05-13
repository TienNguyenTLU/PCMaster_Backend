package com.edu.pcmaster.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.edu.pcmaster.dto.order.OrderItemResponse;
import com.edu.pcmaster.dto.order.OrderRequest;
import com.edu.pcmaster.dto.order.OrderResponse;
import com.edu.pcmaster.models.Order;
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

	private OrderResponse toResponse(Order order) {
		return new OrderResponse(
				order.getId(),
				order.getUser() == null ? null : order.getUser().getId(),
				order.getTotalAmount(),
				order.getStatus(),
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


