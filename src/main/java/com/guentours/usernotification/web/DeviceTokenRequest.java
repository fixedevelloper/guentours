package com.guentours.usernotification.web;

import jakarta.validation.constraints.NotBlank;

/** {@code platform} ("ANDROID"/"IOS") is client-reported and purely informational. */
public record DeviceTokenRequest(@NotBlank String token, String platform) {
}
