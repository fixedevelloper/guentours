package com.guentours.partners.carrental.web;

import com.guentours.partners.carrental.domain.VehicleAvailability;
import java.math.BigDecimal;
import java.time.LocalDate;

public record VehicleAvailabilityResponse(
        String id,
        LocalDate rentDate,
        Integer unitsAvailable,
        BigDecimal priceOverride
) {
    public static VehicleAvailabilityResponse from(VehicleAvailability a) {
        return new VehicleAvailabilityResponse(a.getId(), a.getRentDate(), a.getUnitsAvailable(), a.getPriceOverride());
    }
}
