package com.guentours.security;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Builds the HttpOnly cookie carrying the JWT, so the frontend never has to keep the raw token in
 * JS-reachable storage (localStorage) where any XSS payload could read and exfiltrate it.
 */
@Component
public class AuthCookieService {

    private final JwtProperties properties;

    public AuthCookieService(JwtProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie buildAuthCookie(String token) {
        return baseCookie(token)
                .maxAge(Duration.ofMinutes(properties.getExpirationMinutes()))
                .build();
    }

    public ResponseCookie buildLogoutCookie() {
        return baseCookie("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        // The frontend and API are on different registrable domains in production, so every call
        // the frontend makes (fetch/XHR, e.g. GET /api/auth/me right after the OAuth2 redirect) is
        // cross-site. A SameSite=Lax cookie is only attached to top-level navigations in that case,
        // not to those follow-up requests, so the cookie must be SameSite=None there - which in turn
        // requires Secure (browsers drop None cookies that aren't Secure). Locally, frontend and API
        // share the "localhost" site and APP_COOKIE_SECURE is false (plain http), so this falls back
        // to Lax, matching cookie-secure's existing prod/dev split.
        boolean secure = properties.isCookieSecure();
        return ResponseCookie.from(properties.getCookieName(), value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(secure ? "None" : "Lax")
                .path("/");
    }
}
