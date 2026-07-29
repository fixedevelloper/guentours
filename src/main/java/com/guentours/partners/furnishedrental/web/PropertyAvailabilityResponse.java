package com.guentours.partners.furnishedrental.web;

import com.guentours.partners.furnishedrental.domain.PropertyAvailability;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PropertyAvailabilityResponse(
        String id,
        LocalDate stayDate,
        Boolean isAvailable,
        BigDecimal priceOverride
) {
    public static PropertyAvailabilityResponse from(PropertyAvailability a) {
        return new PropertyAvailabilityResponse(a.getId(), a.getStayDate(), a.getIsAvailable(), a.getPriceOverride());
    }
}
