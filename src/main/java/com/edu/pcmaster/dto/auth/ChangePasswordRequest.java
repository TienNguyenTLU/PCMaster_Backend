package com.edu.pcmaster.dto.auth;

public record ChangePasswordRequest(
    String oldPassword,
    String newPassword
) {
}
