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
        @JsonProperty("room_rates") @JsonAlias({"roomRates", "RoomRates"}) List<RoomOffer> roomRates
) {
    // Helper pour éviter les NullPointerException si roomRates est nul
    public List<RoomOffer> roomRates() {
        return roomRates != null ? roomRates : List.of();
    }
}