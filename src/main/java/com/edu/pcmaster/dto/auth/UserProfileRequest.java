package com.edu.pcmaster.dto.auth;

public record UserProfileRequest(
		String username,
		String phone,
		String address
) {}
