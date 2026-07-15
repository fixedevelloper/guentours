package com.guentours.security;

public record AuthResponse(String token, String tokenType, String email, String fullName, String role) {

    public static AuthResponse of(String token, String email, String fullName, String role) {
        return new AuthResponse(token, "Bearer", email, fullName, role);
    }
}
