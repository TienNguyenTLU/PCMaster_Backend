package com.edu.pcmaster.dto.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.edu.pcmaster.models.DeliveryType;
import com.edu.pcmaster.models.OrderStatus;

public record OrderResponse(
		Long id,
		Long userId,
		String username,
		String email,
		BigDecimal totalAmount,
		OrderStatus status,
		DeliveryType deliveryType,
		String recipientName,
		String recipientPhone,
		String shippingAddress,
		String documentUrl,
		Instant createdAt,
		List<OrderItemResponse> items
) {
}
