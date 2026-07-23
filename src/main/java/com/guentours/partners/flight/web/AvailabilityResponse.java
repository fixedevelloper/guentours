package com.guentours.partners.flight.web;

import com.guentours.partners.flight.domain.DepartureStatus;
import com.guentours.partners.flight.domain.FlightAvailability;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AvailabilityResponse(
        String id,
        LocalDate flightDate,
        Integer seatsAvailable,
        BigDecimal priceOverride,
        DepartureStatus status
) {
    public static AvailabilityResponse from(FlightAvailability a) {
        return new AvailabilityResponse(a.getId(), a.getFlightDate(), a.getSeatsAvailable(),
                a.getPriceOverride(), a.getStatus());
    }
}