/**
 * Notification module: sends transactional emails - auto-provisioned account
 * credentials, booking confirmations with e-ticket references, and failure notices -
 * triggered entirely by domain events from the user/booking modules.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"user", "booking"}
)
package com.guentours.notification;
