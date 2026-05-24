package com.edu.pcmaster.dto.product;

import java.math.BigDecimal;
import java.time.Instant;

import com.edu.pcmaster.dto.brand.BrandResponse;
import com.edu.pcmaster.dto.category.CategoryResponse;

import java.util.List;

public record ProductResponse(
		Long id,
		Long categoryId,
		Long brandId,
		CategoryResponse category,
		BrandResponse brand,
		String name,
		String slug,
		BigDecimal price,
		Integer stock,
		String thumbnailUrl,
		String description,
		String specsJson,
		Instant createdAt,
		Instant updatedAt,
		List<PcComponentResponse> pcComponents
) {
	public record PcComponentResponse(
			Long componentProductId,
			String componentProductName,
			String componentProductThumbnailUrl,
			BigDecimal componentProductPrice,
			int quantity
	) {}
}

