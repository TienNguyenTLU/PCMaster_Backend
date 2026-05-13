package com.edu.pcmaster.dto.build;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PcBuildResponse(
		Long id,
		Long userId,
		String name,
		BigDecimal totalPrice,
		Integer totalPower,
		Instant createdAt,
		List<PcBuildItemResponse> items
) {
}

