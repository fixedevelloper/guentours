/**
 * Partners module: onboarding and inventory (hotels, flights, vehicles, furnished rentals) for
 * every marketplace partner vertical.
 *
 * <p>{@code type = OPEN}: each vertical's domain entities and repositories (e.g.
 * partners.hotel.domain, partners.hotel.repository) are read directly by the {@code provider}
 * module's DIRECT client, which books against this locally-hosted inventory instead of an
 * external GDS - that adapter needs the same breadth of access this module's own web/service
 * layers have, not just a curated subset.
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.guentours.partners;
