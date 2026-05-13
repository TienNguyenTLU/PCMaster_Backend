package com.edu.pcmaster.dto.purchase;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PurchaseOrderItemRequest(
		@NotNull Long productId,
		@NotNull @Min(1) Integer quantity,
		@NotNull @DecimalMin("0.01") BigDecimal importPrice
) {
}


