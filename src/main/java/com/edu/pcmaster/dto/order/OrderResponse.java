package com.edu.pcmaster.dto.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.edu.pcmaster.models.OrderStatus;

public record OrderResponse(
		Long id,
		Long userId,
		BigDecimal totalAmount,
		OrderStatus status,
		Instant createdAt,
		List<OrderItemResponse> items
) {
}

