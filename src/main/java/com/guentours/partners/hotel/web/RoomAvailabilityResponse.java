package com.guentours.partners.hotel.web;

import com.guentours.partners.hotel.domain.RoomAvailability;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RoomAvailabilityResponse(
        String id,
        LocalDate stayDate,
        Integer roomsAvailable,
        BigDecimal priceOverride
) {
    public static RoomAvailabilityResponse from(RoomAvailability a) {
        return new RoomAvailabilityResponse(a.getId(), a.getStayDate(), a.getRoomsAvailable(), a.getPriceOverride());
    }
}