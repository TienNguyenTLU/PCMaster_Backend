package com.edu.pcmaster.dto.promotion;

import java.time.Instant;
import java.util.List;

public record PromotionResponse(
		Long id,
		String name,
		String slug,
		String description,
		String bannerUrl,
		Integer discountPercent,
		Instant startDate,
		Instant endDate,
		Boolean active,
		List<Long> productIds,
		Instant createdAt
) {}
