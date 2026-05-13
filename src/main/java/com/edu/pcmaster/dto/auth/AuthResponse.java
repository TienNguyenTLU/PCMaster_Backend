package com.edu.pcmaster.dto.auth;

import com.edu.pcmaster.models.UserRole;

public record AuthResponse(
		String token,
		Long userId,
		String username,
		String email,
		UserRole role
) {
}

