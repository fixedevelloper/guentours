/**
 * Booking module: owns the booking aggregate and its state machine
 * (PENDING_PAYMENT -&gt; PAID -&gt; CONFIRMING -&gt; CONFIRMED/FAILED), snapshotting
 * the chosen provider offer at checkout time so later steps (payment,
 * provider confirmation) never need to trust client-supplied prices again.
 *
 * <p>{@code type = OPEN}: the booking aggregate (Booking.domain) and its DTOs (Booking.web) are
 * read directly by several downstream modules that report on or react to bookings (payment,
 * commission, destination, ticketing, notification, usernotification, reseller) rather than only
 * through this module's root-package services - reflecting that reality instead of a facade of
 * encapsulation this module doesn't actually have.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"user", "provider", "provider::dto", "search", "shared", "security"},
        type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.guentours.booking;
