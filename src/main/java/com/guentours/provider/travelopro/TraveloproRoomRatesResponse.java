package com.guentours.provider.travelopro;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response body for Travelopro/TravelNext's {@code POST /api/hotel-api-v6/get_room_rates}. Each
 * {@code perBookingRates} entry is a bookable rate for the property; {@code rateBasisId} is the
 * token {@code hotel_book} consumes, and {@code cancellationPolicy} is a {@code |t|}-delimited
 * string of rules.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TraveloproRoomRatesResponse(
        String sessionId,
        String hotelId,
        String tokenId,
        RoomRates roomRates,
        Errors Errors
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RoomRates(
            List<PerBookingRate> perBookingRates
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PerBookingRate(
            String productId,
            String roomType,
            String description,
            String roomCode,
            String fareType,
            String rateBasisId,
            String currency,
            Double netPrice,
            String boardType,
            Integer maxOccupancyPerRoom,
            String inventoryType,
            String cancellationPolicy
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Errors(
            String ErrorCode,
            String ErrorMessage
    ) {
    }
}
