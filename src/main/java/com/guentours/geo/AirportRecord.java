package com.guentours.geo;

/** One airport as returned by an {@link AirportDataSource}, before it's persisted. */
public record AirportRecord(String airportCode, String airportName, String city, String country) {
}
