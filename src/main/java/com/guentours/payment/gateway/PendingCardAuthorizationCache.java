package com.guentours.payment.gateway;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the card details (PAN/CVV/expiry) a card charge needs to complete a PIN/AVS authorization
 * challenge, keyed by payment id, for just long enough for the payer to enter their PIN - in memory
 * only, never written to the database, since the card details are otherwise discarded immediately
 * after the initial charge. An entry vanishes once read (single use) or after {@link #TTL}, whichever
 * comes first, so a stale or abandoned authorization can never be replayed.
 */
@Component
public class PendingCardAuthorizationCache {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();

    public void put(String paymentId, ChargeRequest request) {
        entries.put(paymentId, new Entry(request, Instant.now().plus(TTL)));
    }

    /** Consumes (removes) the cached request if present and not expired. */
    public Optional<ChargeRequest> take(String paymentId) {
        Entry entry = entries.remove(paymentId);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            return Optional.empty();
        }
        return Optional.of(entry.request());
    }

    public void invalidate(String paymentId) {
        entries.remove(paymentId);
    }

    private record Entry(ChargeRequest request, Instant expiresAt) {
    }
}
