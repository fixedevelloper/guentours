/**
 * Destination module: the "popular destinations" shown on the homepage. Rows are suggested
 * automatically from real flight booking volume ({@link FeaturedDestinationService#refreshFromBookings})
 * and then fully editable by an admin (image, display order, active flag) - the auto-suggestion
 * never overwrites an existing entry, it only adds destinations not already tracked.
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"booking", "geo", "shared", "security"}
)
package com.guentours.destination;
