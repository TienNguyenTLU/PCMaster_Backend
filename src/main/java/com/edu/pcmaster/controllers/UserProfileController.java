package com.edu.pcmaster.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.edu.pcmaster.common.exception.BadRequestException;
import com.edu.pcmaster.dto.auth.UserProfileRequest;
import com.edu.pcmaster.dto.auth.UserProfileResponse;
import com.edu.pcmaster.models.User;
import com.edu.pcmaster.repositories.UserRepository;
import com.edu.pcmaster.services.CurrentUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import com.edu.pcmaster.dto.auth.ChangePasswordRequest;

@RestController
@RequestMapping("/api/profile")
public class UserProfileController {

	private final CurrentUserService currentUserService;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserProfileController(CurrentUserService currentUserService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.currentUserService = currentUserService;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping
	public UserProfileResponse getProfile() {
		User user = currentUserService.requireUser();
		return new UserProfileResponse(
				user.getId(),
				user.getUsername(),
				user.getEmail(),
				user.getPhone(),
				user.getAddress(),
				user.getRole().name()
		);
	}

	@PutMapping
	public UserProfileResponse updateProfile(@RequestBody UserProfileRequest request) {
		User user = currentUserService.requireUser();

		String newUsername = request.username() != null ? request.username().trim() : "";
		if (newUsername.isEmpty()) {
			throw new BadRequestException("Tên tài khoản không được để trống");
		}

		if (!user.getUsername().equalsIgnoreCase(newUsername) && userRepository.existsByUsername(newUsername)) {
			throw new BadRequestException("Tên tài khoản đã tồn tại");
		}

		user.setUsername(newUsername);
		user.setPhone(request.phone() != null ? request.phone().trim() : null);
		user.setAddress(request.address() != null ? request.address().trim() : null);

		User saved = userRepository.save(user);

		return new UserProfileResponse(
				saved.getId(),
				saved.getUsername(),
				saved.getEmail(),
				saved.getPhone(),
				saved.getAddress(),
				saved.getRole().name()
		);
	}

	@PutMapping("/password")
	public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
		User user = currentUserService.requireUser();

		if (request.oldPassword() == null || request.oldPassword().isEmpty() ||
			request.newPassword() == null || request.newPassword().isEmpty()) {
			throw new BadRequestException("Mật khẩu không được để trống");
		}

		if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
			throw new BadRequestException("Mật khẩu cũ không chính xác");
		}

		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		userRepository.save(user);

		return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
	}
}
