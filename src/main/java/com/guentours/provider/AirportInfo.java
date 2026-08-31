package com.guentours.provider;

/** One endpoint (departure or arrival) of a {@link FlightSegmentDetail}. */
public record AirportInfo(String code, String name, String city, String terminal) {
}
