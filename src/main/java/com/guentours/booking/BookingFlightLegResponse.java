package com.guentours.booking;

import java.time.LocalDateTime;

public record BookingFlightLegResponse(
        int legIndex,
        String airline,
        String flightNumber,
        String origin,
        String destination,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime
) {
    static BookingFlightLegResponse from(BookingFlightLeg leg) {
        return new BookingFlightLegResponse(leg.getLegIndex(), leg.getAirline(), leg.getFlightNumber(),
                leg.getOrigin(), leg.getDestination(), leg.getDepartureTime(), leg.getArrivalTime());
    }
}
