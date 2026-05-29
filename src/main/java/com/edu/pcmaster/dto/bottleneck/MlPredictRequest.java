package com.edu.pcmaster.dto.bottleneck;

import java.util.Map;

/**
 * Request DTO for calling the Python ML service.
 */
public record MlPredictRequest(
		ComponentSpecs cpu,
		ComponentSpecs gpu,
		String resolution
) {
	public record ComponentSpecs(
			String name,
			Map<String, Object> specs
	) {
	}
}
