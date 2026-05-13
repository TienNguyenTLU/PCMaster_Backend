package com.edu.pcmaster.dto.build;

import jakarta.validation.constraints.NotBlank;

public record PcBuildRequest(
		@NotBlank String name
) {
}

