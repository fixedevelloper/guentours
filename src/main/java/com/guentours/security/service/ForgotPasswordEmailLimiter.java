package com.guentours.security.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class ForgotPasswordEmailLimiter {

    private static final int MAX_REQUESTS = 3;
    private static final long WINDOW_MILLIS = 60L * 60 * 1000; // 1 heure

    private record Entry(int count, long windowStartEpochMilli) {
    }

    private final Map<String, Entry> attempts = new ConcurrentHashMap<>();

    /** @return true si la demande est autorisée, false si la limite est atteinte pour cet email */
    boolean tryConsume(String normalizedEmail) {
        long now = Instant.now().toEpochMilli();

        Entry updated = attempts.compute(normalizedEmail, (key, entry) -> {
            if (entry == null || now - entry.windowStartEpochMilli() > WINDOW_MILLIS) {
                return new Entry(1, now);
            }
            return new Entry(entry.count() + 1, entry.windowStartEpochMilli());
        });

        return updated.count() <= MAX_REQUESTS;
    }
}