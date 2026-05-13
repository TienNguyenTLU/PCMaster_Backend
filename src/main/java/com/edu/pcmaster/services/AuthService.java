package com.edu.pcmaster.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.edu.pcmaster.common.exception.BadRequestException;
import com.edu.pcmaster.dto.auth.AuthLoginRequest;
import com.edu.pcmaster.dto.auth.AuthRegisterRequest;
import com.edu.pcmaster.dto.auth.AuthResponse;
import com.edu.pcmaster.models.User;
import com.edu.pcmaster.models.UserRole;
import com.edu.pcmaster.repositories.UserRepository;
import com.edu.pcmaster.security.JwtTokenProvider;

@Service
public class AuthService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider tokenProvider;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenProvider = tokenProvider;
	}

	public AuthResponse register(AuthRegisterRequest request) {
		if (userRepository.existsByUsername(request.username())) {
			throw new BadRequestException("Username already exists");
		}
		if (userRepository.existsByEmail(request.email())) {
			throw new BadRequestException("Email already exists");
		}

		User user = new User();
		user.setUsername(request.username());
		user.setEmail(request.email());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setRole(UserRole.CUSTOMER);
		User saved = userRepository.save(user);

		String token = tokenProvider.generateToken(saved);
		return new AuthResponse(token, saved.getId(), saved.getUsername(), saved.getEmail(), saved.getRole());
	}

	public AuthResponse login(AuthLoginRequest request) {
		User user = userRepository.findByUsernameOrEmail(request.usernameOrEmail(), request.usernameOrEmail())
				.orElseThrow(() -> new BadRequestException("Invalid credentials"));
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new BadRequestException("Invalid credentials");
		}
		String token = tokenProvider.generateToken(user);
		return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole());
	}
}

