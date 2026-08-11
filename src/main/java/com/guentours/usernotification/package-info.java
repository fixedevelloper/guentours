/**
 * User-notification module: an in-app, real-time inbox for users impacted by something outside
 * their control - a booking the provider couldn't confirm, an automatic cancellation, or a
 * failed payment - pushed live over SSE and persisted with a bounded retention once read
 * (see {@code UserNotificationRetentionCleanup}).
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"booking", "payment", "security", "shared"}
)
package com.guentours.usernotification;
