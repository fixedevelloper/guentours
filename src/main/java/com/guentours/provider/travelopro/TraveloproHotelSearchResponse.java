package com.guentours.provider.travelopro;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response body for Travelopro/TravelNext's {@code POST /api/hotel-api-v6/hotel_search}. Each
 * {@code itinerary} is one hotel with its cheapest bookable product; {@code status.sessionId},
 * {@code productId} and {@code tokenId} are the identifiers a follow-up {@code get_room_rates}/
 * {@code hotel_book} call needs and are carried forward in the offer's provider context.
 * A validation failure surfaces as a top-level {@code Errors} object instead of {@code itineraries}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TraveloproHotelSearchResponse(
        Status status,
        List<Itinerary> itineraries,
        Errors Errors
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Status(
            String sessionId,
            Boolean moreResults,
            String nextToken,
            Integer totalResults,
            String error
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Itinerary(
            String hotelId,
            String twxHotelId,
            String productId,
            String tokenId,
            String hotelName,
            String hotelRating,
            String propertyType,
            String fareType,
            Double total,
            String currency,
            String city,
            String locality,
            String country,
            String address,
            String postalCode,
            String phone,
            String email,
            String latitude,
            String longitude,
            String thumbNailUrl,
            List<String> facilities
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Errors(
            String ErrorCode,
            String ErrorMessage
    ) {
    }
}
