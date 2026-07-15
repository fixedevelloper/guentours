/**
 * Booking module: owns the booking aggregate and its state machine
 * (PENDING_PAYMENT -&gt; PAID -&gt; CONFIRMING -&gt; CONFIRMED/FAILED), snapshotting
 * the chosen provider offer at checkout time so later steps (payment,
 * provider confirmation) never need to trust client-supplied prices again.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"user", "provider", "provider::dto", "search", "shared", "security"}
)
package com.guentours.booking;
