package com.guentours.booking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Pushes booking status changes to any client subscribed to
 * {@code GET /api/bookings/{id}/track} so the "PENDING_PAYMENT -> PAID ->
 * CONFIRMING -> CONFIRMED" journey is visible live instead of via polling.
 */
@Component
class BookingTrackingService {

    private static final Logger log = LoggerFactory.getLogger(BookingTrackingService.class);
    private static final long EMITTER_TIMEOUT_MILLIS = 10L * 60 * 1000;

    private final Map<String, List<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    SseEmitter subscribe(String bookingId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MILLIS);
        List<SseEmitter> emitters = subscribers.computeIfAbsent(bookingId, k -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));
        return emitter;
    }

    void publish(String bookingId, BookingStatus status) {
        List<SseEmitter> emitters = subscribers.get(bookingId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("status").data(status));
                if (status == BookingStatus.CONFIRMED || status == BookingStatus.FAILED
                        || status == BookingStatus.CANCELLED) {
                    emitter.complete();
                }
            } catch (IOException ex) {
                log.debug("Dropping stale booking-tracking subscriber for {}: {}", bookingId, ex.getMessage());
                emitters.remove(emitter);
            }
        }
    }
}
