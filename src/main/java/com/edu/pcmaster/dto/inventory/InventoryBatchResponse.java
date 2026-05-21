package com.edu.pcmaster.dto.inventory;

import java.math.BigDecimal;
import java.time.Instant;

public record InventoryBatchResponse(
		Long id,
		Long productId,
		String productName,
		String thumbnailUrl,
		Integer quantity,
		Integer remainingQuantity,
		BigDecimal importPrice,
		BigDecimal sellingPrice,
		Instant importedAt
) {
}
