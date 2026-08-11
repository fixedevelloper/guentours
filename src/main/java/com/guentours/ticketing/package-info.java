/**
 * Ticketing module: reacts to a confirmed booking by generating one electronic
 * ticket per traveler/e-ticket number the provider issued.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"booking", "reseller::domain", "storage", "shared"}
)
package com.guentours.ticketing;
