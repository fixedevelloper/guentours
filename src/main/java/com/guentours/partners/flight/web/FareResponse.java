package com.guentours.partners.flight.web;

import com.guentours.partners.flight.domain.CabinClass;
import com.guentours.partners.flight.domain.FlightFare;
import java.math.BigDecimal;

public record FareResponse(
        String id,
        CabinClass cabinClass,
        BigDecimal basePrice,
        String currency,
        Integer baggageAllowanceKg,
        Integer totalSeats
) {
    public static FareResponse from(FlightFare f) {
        return new FareResponse(f.getId(), f.getCabinClass(), f.getBasePrice(),
                f.getCurrency(), f.getBaggageAllowanceKg(), f.getTotalSeats());
    }
}