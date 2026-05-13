package com.edu.pcmaster.dto.build;

import com.edu.pcmaster.models.ComponentType;

public record PcBuildItemResponse(
		Long id,
		Long productId,
		ComponentType componentType
) {
}

