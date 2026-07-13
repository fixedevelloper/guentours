package com.guentours.provider;

import java.time.LocalDate;

public record FlightSearchCriteria(
        String origin,
        String destination,
        LocalDate departureDate,
        LocalDate returnDate,
        int adults,
        int children,
        int infants,
        JourneyType journeyType,
        String cabinClass,
        String currency
) {
}