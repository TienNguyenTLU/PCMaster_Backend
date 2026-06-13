package com.edu.pcmaster.dto.auth;

public record UserProfileResponse(
		Long id,
		String username,
		String email,
		String phone,
		String address,
		String role
) {}
