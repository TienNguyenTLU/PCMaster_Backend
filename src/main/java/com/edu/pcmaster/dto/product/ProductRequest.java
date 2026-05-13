package com.edu.pcmaster.dto.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductRequest(
		@NotNull Long categoryId,
		@NotNull Long brandId,
		@NotBlank String name,
		@NotBlank String slug,
		@NotNull BigDecimal price,
		String thumbnailUrl,
		String description,
		String specsJson
) {
}


