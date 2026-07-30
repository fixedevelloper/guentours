/**
 * Reference data module: airports (IATA codes) and hotel cities used to power
 * origin/destination/city autocomplete on the frontend, and to resolve a searched
 * hotel city's coordinates for {@code search} (see its {@code allowedDependencies}).
 * Depends on nothing beyond the JDK/Spring.
 */
@org.springframework.modulith.ApplicationModule
package com.guentours.geo;
