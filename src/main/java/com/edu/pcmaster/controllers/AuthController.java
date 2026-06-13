package com.edu.pcmaster.controllers;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.edu.pcmaster.dto.auth.AuthGoogleRequest;
import com.edu.pcmaster.dto.auth.AuthLoginRequest;
import com.edu.pcmaster.dto.auth.AuthRegisterRequest;
import com.edu.pcmaster.dto.auth.AuthResponse;
import com.edu.pcmaster.services.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	public AuthResponse register(@Valid @RequestBody AuthRegisterRequest request) {
		return authService.register(request);
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody AuthLoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/google")
	public AuthResponse googleLogin(@Valid @RequestBody AuthGoogleRequest request) {
		return authService.loginWithGoogle(request);
	}
}


