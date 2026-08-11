package com.guentours.user.service;

import java.util.Optional;

/**
 * Port owned by the {@code user} module (the consumer, {@code NotificationEventListener}) rather
 * than by {@code security} (the implementer, {@code PasswordResetService}) - same rationale as
 * {@link PartnerWelcomeNotifier}: notification already depends on user elsewhere, so this
 * direction adds no new module edge, whereas notification depending directly on
 * {@code security.service.PasswordResetService} closed a Modulith cycle
 * (notification -> security -> user -> notification).
 */
public interface PendingPasswordResetLinkSource {
    /**
     * Single-use: consumes and returns the reset link cached for this user, or empty if none is
     * pending (already consumed, or the event was published before the link was cached).
     */
    Optional<String> consumePendingResetLink(String userId);
}
