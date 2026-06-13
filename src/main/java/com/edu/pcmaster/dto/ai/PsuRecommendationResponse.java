package com.edu.pcmaster.dto.ai;

public record PsuRecommendationResponse(
		int recommendedWattage,
		String explanation
) {
}
