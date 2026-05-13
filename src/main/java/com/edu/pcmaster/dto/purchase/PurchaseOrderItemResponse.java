package com.edu.pcmaster.dto.purchase;

import java.math.BigDecimal;

public record PurchaseOrderItemResponse(
		Long id,
		Long productId,
		Integer quantity,
		BigDecimal importPrice
) {
}

