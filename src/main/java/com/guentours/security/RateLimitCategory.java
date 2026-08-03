package com.guentours.security;

/** Which {@link RateLimitProperties.Bucket} applies to a request, chosen by {@link RateLimitFilter} from the path. */
public enum RateLimitCategory {
    AUTH,
    BOOKING,
    GENERAL
}
