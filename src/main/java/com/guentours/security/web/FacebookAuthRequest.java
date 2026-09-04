package com.guentours.security.web;

import jakarta.validation.constraints.NotBlank;

/** Access token obtained natively (flutter_facebook_auth on Flutter) - verified server-side (and
 *  used to fetch the profile) before any account is resolved/created, see
 *  {@code AuthController#loginWithFacebook}. */
public record FacebookAuthRequest(@NotBlank String accessToken) {
}
