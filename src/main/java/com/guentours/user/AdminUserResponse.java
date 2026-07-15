package com.guentours.user;

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
                user.getRole(), user.isAutoProvisioned(), user.getCreatedAt());
    }
}
