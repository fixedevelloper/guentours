package com.guentours.provider;

import java.time.LocalDateTime;
import java.util.List;

/**
 * One physical flight segment inside a {@link FlightOfferDetail}. Two or more segments for the
 * same leg means a stopover between {@code departure} and the previous segment's {@code arrival}
 * ({@code layoverAfter} is that gap's duration, provider-formatted e.g. {@code "2h 45m"}).
 */
public record FlightSegmentDetail(
        String airlineCode,
        String airlineName,
        String flightNumber,
        String cabinClass,
        AirportInfo departure,
        AirportInfo arrival,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        String duration,
        String layoverAfter,
        List<BaggageRule> cabinBaggage,
        List<BaggageRule> checkedBaggage
) {
}
