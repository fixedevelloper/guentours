package com.guentours.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token-bucket rate limiter, one bucket per (client key, category). Deliberately not
 * backed by Redis/a shared store: the app runs as a single instance (see docker-compose), so a
 * process-local map is both simpler and sufficient - if this ever runs behind multiple
 * instances, each would enforce its own independent limit rather than a combined one, which is a
 * looser (not broken) limit, not a security hole.
 */
@Service
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimiterService {

    private final RateLimitProperties properties;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimiterService(RateLimitProperties properties) {
        this.properties = properties;
    }

    public RateLimitResult tryConsume(String clientKey, RateLimitCategory category) {
        RateLimitProperties.Bucket config = configFor(category);
        String key = category.name() + ":" + clientKey;
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(config.getCapacity()));
        return bucket.tryConsume(config.getCapacity(), config.getRefillPeriodSeconds());
    }

    private RateLimitProperties.Bucket configFor(RateLimitCategory category) {
        return switch (category) {
            case AUTH -> properties.getAuth();
            case BOOKING -> properties.getBooking();
            case GENERAL -> properties.getGeneral();
        };
    }

    /** Evicts buckets untouched for a while so long-lived deployments don't accumulate one entry
     *  per distinct client IP forever. */
    @Scheduled(fixedRate = 10 * 60 * 1000)
    void evictStaleBuckets() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(30));
        buckets.entrySet().removeIf(entry -> entry.getValue().lastAccess.isBefore(cutoff));
    }

    private static final class TokenBucket {
        private double tokens;
        private Instant lastRefill;
        private volatile Instant lastAccess;

        TokenBucket(int initialCapacity) {
            this.tokens = initialCapacity;
            this.lastRefill = Instant.now();
            this.lastAccess = this.lastRefill;
        }

        synchronized RateLimitResult tryConsume(int capacity, int refillPeriodSeconds) {
            Instant now = Instant.now();
            lastAccess = now;
            double elapsedSeconds = Duration.between(lastRefill, now).toNanos() / 1_000_000_000.0;
            double refillRate = (double) capacity / refillPeriodSeconds;
            tokens = Math.min(capacity, tokens + elapsedSeconds * refillRate);
            lastRefill = now;

            if (tokens >= 1.0) {
                tokens -= 1.0;
                return RateLimitResult.allow();
            }
            double secondsUntilNextToken = (1.0 - tokens) / refillRate;
            return RateLimitResult.deny((long) Math.ceil(secondsUntilNextToken));
        }
    }
}
