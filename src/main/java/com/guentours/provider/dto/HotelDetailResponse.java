package com.guentours.provider.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.guentours.provider.HotelDetail;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HotelDetailResponse(
        @JsonProperty("status") String status,
        @JsonProperty("message") String message,
        @JsonProperty("data") HotelDetail hotelDetail,
        @JsonProperty("errors") Object errors
) {
    /**
     * Méthode utilitaire pour extraire directement le détail si la réponse est valide.
     */
    public HotelDetail getHotelDetail() {
        return hotelDetail;
    }
}