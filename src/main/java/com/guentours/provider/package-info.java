/**
 * Provider module: the abstraction over external GDS/travel APIs (Travelopro, Sabre,
 * Travelport). Exposes a single {@code TravelProviderClient} SPI so the search and
 * booking modules never depend on a specific vendor's wire format.
 */
@org.springframework.modulith.ApplicationModule
package com.guentours.provider;
