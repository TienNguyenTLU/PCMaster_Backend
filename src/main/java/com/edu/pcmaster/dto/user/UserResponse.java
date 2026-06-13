package com.edu.pcmaster.dto.user;

import com.edu.pcmaster.models.UserRole;
import java.time.Instant;

public record UserResponse(
    Long id,
    String username,
    String email,
    UserRole role,
    String phone,
    String address,
    boolean active,
    Instant createdAt
) {}
