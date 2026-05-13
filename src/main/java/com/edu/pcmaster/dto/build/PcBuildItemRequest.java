package com.edu.pcmaster.dto.build;

import com.edu.pcmaster.models.ComponentType;

import jakarta.validation.constraints.NotNull;

public record PcBuildItemRequest(
		@NotNull Long productId,
		@NotNull ComponentType componentType
) {
}

