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
        return ResponseCookie.from(properties.getCookieName(), value)
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite("Lax")
                .path("/");
    }
}
