package com.guentours.security.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

class ResetLinkCache {

    private static final long TTL_MILLIS = 5L * 60 * 1000;

    private record Entry(String link, long expiresAtEpochMilli) {
    }

    private final Map<String, Entry> links = new ConcurrentHashMap<>();

    void store(String userId, String link) {
        links.put(userId, new Entry(link, Instant.now().toEpochMilli() + TTL_MILLIS));
    }

    Optional<String> consume(String userId) {
        Entry entry = links.remove(userId);
        if (entry == null || Instant.now().toEpochMilli() > entry.expiresAtEpochMilli()) {
            return Optional.empty();
        }
        return Optional.of(entry.link());
    }
}