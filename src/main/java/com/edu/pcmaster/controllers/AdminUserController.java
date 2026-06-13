package com.edu.pcmaster.controllers;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.edu.pcmaster.common.exception.BadRequestException;
import com.edu.pcmaster.dto.user.CreateStaffRequest;
import com.edu.pcmaster.dto.user.UserResponse;
import com.edu.pcmaster.models.User;
import com.edu.pcmaster.models.UserRole;
import com.edu.pcmaster.repositories.UserRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public AdminUserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping
	public List<UserResponse> listAllUsers() {
		return userRepository.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	@PostMapping("/create-staff")
	public ResponseEntity<UserResponse> createStaff(@Valid @RequestBody CreateStaffRequest request) {
		if (userRepository.existsByUsername(request.username())) {
			throw new BadRequestException("Username đã tồn tại");
		}
		if (userRepository.existsByEmail(request.email())) {
			throw new BadRequestException("Email đã tồn tại");
		}

		User user = new User();
		user.setUsername(request.username());
		user.setEmail(request.email());
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setRole(UserRole.STAFF);
		user.setPhone(request.phone());
		user.setAddress(request.address());
		user.setActive(true);

		User saved = userRepository.save(user);
		return ResponseEntity.ok(toResponse(saved));
	}

	@PutMapping("/{id}/toggle-status")
	public ResponseEntity<UserResponse> toggleStatus(@PathVariable Long id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new BadRequestException("User không tồn tại"));

		if (user.getRole() == UserRole.ADMIN) {
			throw new BadRequestException("Không thể khóa tài khoản Administrator");
		}

		user.setActive(!user.isActive());
		User saved = userRepository.save(user);
		return ResponseEntity.ok(toResponse(saved));
	}

	private UserResponse toResponse(User user) {
		return new UserResponse(
				user.getId(),
				user.getUsername(),
				user.getEmail(),
				user.getRole(),
				user.getPhone(),
				user.getAddress(),
				user.isActive(),
				user.getCreatedAt()
		);
	}
}
