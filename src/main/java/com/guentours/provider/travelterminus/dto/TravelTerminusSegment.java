package com.guentours.provider.travelterminus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** One flight segment inside {@code route.flightSegments[leg][]}. {@code departure}/{@code arrival}
 *  are single-element arrays in every observed response - kept as lists to match the vendor's shape
 *  exactly rather than guessing they'll always have exactly one entry. {@code noOfSeatAvailable} is
 *  only populated by Revalidate, not Search. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TravelTerminusSegment(
        String airlineCode,
        String airlineName,
        String cabinClass,
        String bookingCode,
        String flightNumber,
        String segmentDuration,
        List<AirportPoint> departure,
        List<AirportPoint> arrival,
        String segmentInterval,
        Integer noOfSeatAvailable
) {
    /** {@code date} is {@code YYYY-MM-DD}; {@code time} is 12h clock with AM/PM (e.g. {@code "3:55 PM"}),
     *  both local to this airport. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AirportPoint(
            String code,
            String name,
            String country,
            String countryCode,
            String city,
            String cityCode,
            String date,
            String time,
            String terminal
    ) {
    }
}
