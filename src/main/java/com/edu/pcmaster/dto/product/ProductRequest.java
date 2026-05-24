package com.edu.pcmaster.dto.product;

import java.math.BigDecimal;
import java.util.List;

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
		String specsJson,
		Integer stock,
		List<PcComponentRequest> pcComponents
) {
	public record PcComponentRequest(
		@NotNull Long componentProductId,
		@NotNull int quantity
	) {}
}
