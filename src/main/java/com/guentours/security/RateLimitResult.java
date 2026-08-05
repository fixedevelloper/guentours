package com.guentours.security;

/** @param retryAfterSeconds Only meaningful when {@code allowed} is false - ceil'd, never 0, so
 *                           a client always gets a positive Retry-After. */
public record RateLimitResult(boolean allowed, long retryAfterSeconds) {

    public static RateLimitResult allow() {
        return new RateLimitResult(true, 0);
    }

    public static RateLimitResult deny(long retryAfterSeconds) {
        return new RateLimitResult(false, Math.max(1, retryAfterSeconds));
    }
}
