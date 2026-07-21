package com.guentours.provider.travelopro;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Request body for Travelopro/TravelNext's {@code POST /api/hotel-api-v6/hotel_search} endpoint.
 * Aligned with the vendor's Hotel API v6 contract: shared {@code user_id/user_password/access/
 * ip_address} auth plus the stay/occupancy criteria. A city-name search is used here; the API also
 * accepts {@code latitude/longitude/radius} or {@code hotelCodes}, left null (and thus omitted) for
 * a plain city search.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TraveloproHotelSearchRequest(
        String user_id,
        String user_password,
        String access,
        String ip_address,
        String requiredCurrency,
        String nationality,
        String checkin,
        String checkout,
        String city_name,
        String country_name,
        Integer radius,
        Integer maxResult,
        List<Occupancy> occupancy
) {

    /** One entry per room; {@code child_age} holds one age per child in the room. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Occupancy(
            int room_no,
            int adult,
            int child,
            List<Integer> child_age
    ) {
    }
}
