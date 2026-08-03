package com.guentours.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterServiceTest {

    @Test
    void allowsUpToCapacityThenDeniesWithAPositiveRetryAfter() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setAuth(new RateLimitProperties.Bucket(3, 60));
        RateLimiterService service = new RateLimiterService(properties);

        for (int i = 0; i < 3; i++) {
            assertThat(service.tryConsume("client-a", RateLimitCategory.AUTH).allowed()).isTrue();
        }

        RateLimitResult fourth = service.tryConsume("client-a", RateLimitCategory.AUTH);
        assertThat(fourth.allowed()).isFalse();
        assertThat(fourth.retryAfterSeconds()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void tracksSeparateBucketsPerClientKey() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setAuth(new RateLimitProperties.Bucket(1, 60));
        RateLimiterService service = new RateLimiterService(properties);

        assertThat(service.tryConsume("client-a", RateLimitCategory.AUTH).allowed()).isTrue();
        assertThat(service.tryConsume("client-a", RateLimitCategory.AUTH).allowed()).isFalse();
        // A different client must not be affected by client-a's exhausted bucket.
        assertThat(service.tryConsume("client-b", RateLimitCategory.AUTH).allowed()).isTrue();
    }

    @Test
    void tracksSeparateBucketsPerCategoryForTheSameClient() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setAuth(new RateLimitProperties.Bucket(1, 60));
        properties.setBooking(new RateLimitProperties.Bucket(1, 60));
        RateLimiterService service = new RateLimiterService(properties);

        assertThat(service.tryConsume("client-a", RateLimitCategory.AUTH).allowed()).isTrue();
        assertThat(service.tryConsume("client-a", RateLimitCategory.AUTH).allowed()).isFalse();
        // Exhausting the AUTH bucket must not affect the same client's BOOKING bucket.
        assertThat(service.tryConsume("client-a", RateLimitCategory.BOOKING).allowed()).isTrue();
    }

    @Test
    void refillsGraduallyOverTime() throws InterruptedException {
        RateLimitProperties properties = new RateLimitProperties();
        // 10 tokens/second refill rate, easy to observe within a short sleep.
        properties.setGeneral(new RateLimitProperties.Bucket(1, 1));
        RateLimiterService service = new RateLimiterService(properties);

        assertThat(service.tryConsume("client-a", RateLimitCategory.GENERAL).allowed()).isTrue();
        assertThat(service.tryConsume("client-a", RateLimitCategory.GENERAL).allowed()).isFalse();

        Thread.sleep(1100);

        assertThat(service.tryConsume("client-a", RateLimitCategory.GENERAL).allowed()).isTrue();
    }
}
