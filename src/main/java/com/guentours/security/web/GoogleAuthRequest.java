package com.guentours.security.web;

import jakarta.validation.constraints.NotBlank;

/** ID token obtained natively (google_sign_in on Flutter) - verified server-side before any
 *  account is resolved/created, see {@code AuthController#loginWithGoogle}. */
public record GoogleAuthRequest(@NotBlank String idToken) {
}
