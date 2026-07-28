package com.guentours.provider.travelopro;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.guentours.provider.RoomOffer;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TraveloproRoomRateResponse(
        @JsonProperty("sessionId") String sessionId,
        @JsonProperty("hotelId") String hotelId,
        @JsonProperty("tokenId") String tokenId,
        @JsonProperty("room_rates") @JsonAlias({"roomRates", "RoomRates"}) RoomRatesContainer roomRatesContainer
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RoomRatesContainer(
            @JsonProperty("perBookingRates") @JsonAlias("per_booking_rates") List<RoomOffer> perBookingRates
    ) {
        public List<RoomOffer> perBookingRates() {
            return perBookingRates != null ? perBookingRates : List.of();
        }
    }

    /**
     * Facilite l'accès direct aux offres sans exposer le conteneur JSON
     */
    public List<RoomOffer> getRoomOffers() {
        return (roomRatesContainer != null) ? roomRatesContainer.perBookingRates() : List.of();
    }
}