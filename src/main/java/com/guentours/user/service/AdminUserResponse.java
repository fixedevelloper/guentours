package com.guentours.user.service;

import com.guentours.user.domain.User;
import com.guentours.user.domain.Role;

import java.time.Instant;

public record AdminUserResponse(
        String id,
        String email,
        String fullName,
        String phone,
        Role role,
        boolean autoProvisioned,
        Instant createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getPhone(),
                user.getRole(), user.isMustChangePassword(), user.getCreatedAt());
    }
}
