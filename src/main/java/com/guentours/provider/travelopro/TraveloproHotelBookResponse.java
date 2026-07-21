package com.guentours.provider.travelopro;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response body for Travelopro/TravelNext's {@code POST /api/hotel-api-v6/hotel_book}. A successful
 * booking comes back with {@code status == "CONFIRMED"} plus a {@code referenceNum} (our reference)
 * and {@code supplierConfirmationNum} (the hotel's). Failures surface either as a flat {@code error}
 * string or a top-level {@code Errors} object.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TraveloproHotelBookResponse(
        String status,
        String supplierConfirmationNum,
        String referenceNum,
        String clientRefNum,
        String productId,
        RoomBookDetails roomBookDetails,
        String error,
        Errors Errors
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RoomBookDetails(
            String hotelId,
            String checkIn,
            String checkOut,
            String days,
            String currency,
            String NetPrice,
            String fareType
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Errors(
            String ErrorCode,
            String ErrorMessage
    ) {
    }
}
