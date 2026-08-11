package com.guentours.user.service;

/**
 * Port owned by the {@code user} module (the consumer, via {@link UserService}) rather than by
 * {@code notification} (the implementer, {@code EmailPartnerWelcomeNotifier}) - notification
 * already depends on user elsewhere (NotificationEventListener), so this direction adds no new
 * module edge; the reverse (this interface living in {@code notification.service}, as it used to)
 * made {@code user} depend on {@code notification} and closed a Modulith cycle.
 */
public interface PartnerWelcomeNotifier {
    void sendPartnerWelcomeEmail(String email, String contactName, String companyName, String tempPassword);
}
