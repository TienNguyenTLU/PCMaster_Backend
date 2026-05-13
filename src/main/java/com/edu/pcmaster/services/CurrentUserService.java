package com.edu.pcmaster.services;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.edu.pcmaster.common.exception.ResourceNotFoundException;
import com.edu.pcmaster.models.User;
import com.edu.pcmaster.repositories.UserRepository;
import com.edu.pcmaster.security.UserPrincipal;

@Service
public class CurrentUserService {
	private final UserRepository userRepository;

	public CurrentUserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public User requireUser() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (!(principal instanceof UserPrincipal userPrincipal)) {
			throw new ResourceNotFoundException("User not found");
		}
		return userRepository.findById(userPrincipal.getUser().getId())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
	}
}

