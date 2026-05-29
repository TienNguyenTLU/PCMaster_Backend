package com.edu.pcmaster.dto.bottleneck;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.edu.pcmaster.models.BottleneckSide;

public record BottleneckResponse(
		Long id,
		Long cpuProductId,
		Long gpuProductId,
		String cpuName,
		String gpuName,
		String resolution,
		BigDecimal bottleneckPercent,
		BottleneckSide bottleneckSide,
		Integer fpsEstimate,
		List<String> recommendations,
		Map<String, Object> details
) {
}
