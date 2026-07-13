package com.guentours.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank String fullName,
        String phone,
        @NotBlank @Size(min = 8, message = "password must be at least 8 characters") String password
) {
}
