package com.edu.pcmaster.dto.chat;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
		@NotBlank String message
) {
}

