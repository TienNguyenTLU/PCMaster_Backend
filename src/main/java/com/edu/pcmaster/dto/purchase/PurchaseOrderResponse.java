package com.edu.pcmaster.dto.purchase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.edu.pcmaster.models.PurchaseOrderStatus;

public record PurchaseOrderResponse(
		Long id,
		Long supplierId,
		Long createdBy,
		PurchaseOrderStatus status,
		BigDecimal totalAmount,
		Instant createdAt,
		String documentUrl,
		List<PurchaseOrderItemResponse> items
) {
}
