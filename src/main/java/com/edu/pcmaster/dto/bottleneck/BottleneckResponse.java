package com.edu.pcmaster.dto.bottleneck;

import java.math.BigDecimal;

import com.edu.pcmaster.models.BottleneckSide;

public record BottleneckResponse(
		Long id,
		Long cpuProductId,
		Long gpuProductId,
		String resolution,
		BigDecimal bottleneckPercent,
		BottleneckSide bottleneckSide,
		Integer fpsEstimate
) {
}

