package com.guentours.provider.travelopro;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TraveloproRoomRateRequest(
        @JsonProperty("sessionId") String sessionId,
        @JsonProperty("hotelId") String hotelId,
        @JsonProperty("productId") String productId,
        @JsonProperty("tokenId") String tokenId
) {}
