package com.guentours.newsletter.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record NewsletterSubscribeRequest(
        @NotBlank @Email String email,
        String source
) {
}
