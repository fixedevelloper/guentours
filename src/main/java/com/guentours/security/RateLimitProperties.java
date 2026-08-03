package com.guentours.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Per-category request limits enforced by {@link RateLimitFilter}. Each category is a token
 * bucket: {@code capacity} tokens refilling to full over {@code refillPeriodSeconds}, so a client
 * can burst up to {@code capacity} requests immediately and then sustains roughly
 * {@code capacity / refillPeriodSeconds} requests/second after that.
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    /** Login/register - the most brute-forceable endpoints, so the strictest bucket. */
    private Bucket auth = new Bucket(10, 60);

    /** Checkout - generous enough for a real shopper retrying a form, tight enough to blunt a bot. */
    private Bucket booking = new Bucket(30, 60);

    /** Everything else under /api/**. */
    private Bucket general = new Bucket(300, 60);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Bucket getAuth() {
        return auth;
    }

    public void setAuth(Bucket auth) {
        this.auth = auth;
    }

    public Bucket getBooking() {
        return booking;
    }

    public void setBooking(Bucket booking) {
        this.booking = booking;
    }

    public Bucket getGeneral() {
        return general;
    }

    public void setGeneral(Bucket general) {
        this.general = general;
    }

    public static class Bucket {
        private int capacity;
        private int refillPeriodSeconds;

        public Bucket() {
        }

        public Bucket(int capacity, int refillPeriodSeconds) {
            this.capacity = capacity;
            this.refillPeriodSeconds = refillPeriodSeconds;
        }

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public int getRefillPeriodSeconds() {
            return refillPeriodSeconds;
        }

        public void setRefillPeriodSeconds(int refillPeriodSeconds) {
            this.refillPeriodSeconds = refillPeriodSeconds;
        }
    }
}
