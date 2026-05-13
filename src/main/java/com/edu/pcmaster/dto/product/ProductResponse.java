package com.edu.pcmaster.dto.product;

import java.math.BigDecimal;
import java.time.Instant;

import com.edu.pcmaster.dto.brand.BrandResponse;
import com.edu.pcmaster.dto.category.CategoryResponse;

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
		Instant updatedAt
) {
}

