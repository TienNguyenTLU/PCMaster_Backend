package com.edu.pcmaster.dto.order;

import java.util.List;

import com.edu.pcmaster.models.DeliveryType;
import com.edu.pcmaster.models.PaymentMethod;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record OrderRequest(
		@NotEmpty @Valid List<OrderItemRequest> items,
		@NotNull DeliveryType deliveryType,
		String recipientName,
		String recipientPhone,
		String shippingAddress,
		String couponCode,
		PaymentMethod paymentMethod,
		java.time.Instant appointmentTime
) {
}
