package com.guentours.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * Base64-encoded HMAC secret. No default on purpose: a hardcoded fallback here would be
     * committed to git and known to anyone reading the source, letting them forge admin tokens
     * the moment APP_JWT_SECRET is left unset. Must be supplied via that env var; the app fails
     * to start otherwise.
     */
    private String secret;

    private long expirationMinutes = 60;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMinutes() {
        return expirationMinutes;
    }

    public void setExpirationMinutes(long expirationMinutes) {
        this.expirationMinutes = expirationMinutes;
    }
}
