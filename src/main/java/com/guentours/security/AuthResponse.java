package com.guentours.security;

public record AuthResponse(String token, String tokenType, String email, String fullName) {

    public static AuthResponse of(String token, String email, String fullName) {
        return new AuthResponse(token, "Bearer", email, fullName);
    }
}
