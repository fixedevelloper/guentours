package com.guentours.provider.travelopro;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request body for Travelopro/TravelNext's {@code POST /api/hotel-api-v6/get_room_rates}. Re-prices
 * a hotel product searched earlier and returns its bookable room rates (each with the
 * {@code rateBasisId} required by {@code hotel_book}). All four identifiers come from the search
 * result carried in the offer's provider context.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TraveloproRoomRatesRequest(
        String user_id,
        String user_password,
        String access,
        String ip_address,
        String sessionId,
        String productId,
        String tokenId,
        String hotelId
) {
}
