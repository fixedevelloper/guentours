package com.guentours.user;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds a just-generated temporary password in memory, keyed by user id, until the
 * notification module reads it exactly once to email it out. Never touches a
 * database or Spring's event publication registry, so the plaintext password is
 * never persisted or serialized anywhere - see {@link UserAutoProvisionedEvent}.
 */
@Component
class TemporaryPasswordCache {

    private static final long TTL_MILLIS = 5L * 60 * 1000;

    private record Entry(String password, long expiresAtEpochMilli) {
    }

    private final Map<String, Entry> passwords = new ConcurrentHashMap<>();

    void store(String userId, String password) {
        passwords.put(userId, new Entry(password, Instant.now().toEpochMilli() + TTL_MILLIS));
    }

    Optional<String> consume(String userId) {
        Entry entry = passwords.remove(userId);
        if (entry == null || Instant.now().toEpochMilli() > entry.expiresAtEpochMilli()) {
            return Optional.empty();
        }
        return Optional.of(entry.password());
    }
}
