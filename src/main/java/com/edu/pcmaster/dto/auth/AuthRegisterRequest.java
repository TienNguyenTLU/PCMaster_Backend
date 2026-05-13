package com.edu.pcmaster.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AuthRegisterRequest(
		@NotBlank String username,
		@Email @NotBlank String email,
		@NotBlank String password
) {
}

