package com.edu.pcmaster.dto.promotion;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PromotionRequest(
		@NotBlank String name,
		@NotBlank String slug,
		String description,
		String bannerUrl,
		@NotNull @Min(0) @Max(100) Integer discountPercent,
		@NotNull Instant startDate,
		@NotNull Instant endDate,
		Boolean active,
		List<Long> productIds
) {}
