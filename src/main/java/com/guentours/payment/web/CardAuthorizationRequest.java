package com.guentours.payment.web;

import jakarta.validation.constraints.NotBlank;

public record CardAuthorizationRequest(@NotBlank String pin) {
}
