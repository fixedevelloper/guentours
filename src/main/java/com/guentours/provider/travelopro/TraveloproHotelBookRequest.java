package com.guentours.provider.travelopro;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Request body for Travelopro/TravelNext's {@code POST /api/hotel-api-v6/hotel_book}. Confirms a
 * specific room rate ({@code rateBasisId}) for the searched product and passes the guests per room.
 * TravelNext hotel bookings are immediate (the response comes back {@code CONFIRMED}); there is no
 * separate hold step, so this call is issued once the guest details are known.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TraveloproHotelBookRequest(
        String user_id,
        String user_password,
        String access,
        String ip_address,
        String sessionId,
        String productId,
        String tokenId,
        String rateBasisId,
        String clientRef,
        String customerEmail,
        String customerPhone,
        String bookingNote,
        List<PaxDetail> paxDetails
) {

    /** Guests for one room; the {@code adult}/{@code child} name parts are parallel arrays. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PaxDetail(
            int room_no,
            PaxNames adult,
            PaxNames child
    ) {
    }

    /** Column-oriented guest names, as the vendor expects: one entry per guest across each array. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PaxNames(
            List<String> title,
            List<String> firstName,
            List<String> lastName
    ) {
    }
}
