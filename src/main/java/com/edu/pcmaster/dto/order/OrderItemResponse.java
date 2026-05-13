package com.edu.pcmaster.dto.order;

import java.math.BigDecimal;

public record OrderItemResponse(
		Long id,
		Long productId,
		Integer quantity,
		BigDecimal sellingPrice,
		BigDecimal costPrice
) {
}

