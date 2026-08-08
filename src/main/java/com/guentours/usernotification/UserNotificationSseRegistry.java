package com.guentours.usernotification;

import com.guentours.usernotification.web.UserNotificationResponse;
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
 * Pushes new notifications live to any client subscribed to {@code GET /api/notifications/stream},
 * keyed by user id. Modeled on {@code booking.BookingTrackingService}, but unlike a booking's
 * tracking channel (which completes once the booking reaches a terminal status), a user's
 * notification stream has no terminal state - the emitter stays open until it times out, errors,
 * or the client disconnects.
 */
@Component
class UserNotificationSseRegistry {

    private static final Logger log = LoggerFactory.getLogger(UserNotificationSseRegistry.class);
    private static final long EMITTER_TIMEOUT_MILLIS = 10L * 60 * 1000;

    private final Map<String, List<SseEmitter>> subscribers = new ConcurrentHashMap<>();

    SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MILLIS);
        List<SseEmitter> emitters = subscribers.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(ex -> emitters.remove(emitter));
        return emitter;
    }

    void publish(String userId, UserNotificationResponse notification) {
        List<SseEmitter> emitters = subscribers.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(notification));
            } catch (IOException ex) {
                log.debug("Dropping stale notification subscriber for user {}: {}", userId, ex.getMessage());
                emitters.remove(emitter);
            }
        }
    }
}
