package com.edu.pcmaster.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record AuthGoogleRequest(
		@NotBlank String idToken
) {
}
