package com.edu.pcmaster.dto.bottleneck;

import java.util.List;
import java.util.Map;


public record MlPredictResponse(
		double bottleneck_percent,
		String bottleneck_side,
		int fps_estimate,
		double cpu_score_used,
		double gpu_score_used,
		List<String> recommendations,
		Map<String, Object> details
) {
}
