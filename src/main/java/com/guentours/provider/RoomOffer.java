package com.guentours.provider;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RoomOffer(
        @JsonProperty("productId") @JsonAlias("product_id") String productId,
        @JsonProperty("roomType") @JsonAlias({"room_type", "RoomType"}) String roomType,
        @JsonProperty("description") String description,
        @JsonProperty("roomCode") @JsonAlias("room_code") String roomCode,
        @JsonProperty("fareType") @JsonAlias("fare_type") String fareType,
        @JsonProperty("rateBasisId") @JsonAlias("rate_basis_id") String rateBasisId,
        @JsonProperty("currency") String currency,
        @JsonProperty("netPrice") @JsonAlias({"net_price", "NetPrice", "price"}) BigDecimal netPrice,
        @JsonProperty("boardType") @JsonAlias({"board_type", "BoardType"}) String boardType,
        @JsonProperty("maxOccupancyPerRoom") @JsonAlias({"max_occupancy", "maxOccupancy"}) Integer maxOccupancyPerRoom,
        @JsonProperty("inventoryType") @JsonAlias("inventory_type") String inventoryType,
        @JsonProperty("cancellationPolicy") @JsonAlias({"cancellation_policy", "CancellationPolicy"}) String cancellationPolicy,
        @JsonProperty("roomImages") @JsonAlias({"room_images", "images"}) List<String> roomImages,
        @JsonProperty("facilities") @JsonAlias("amenities") List<String> facilities
) {
    // Protection fallback pour les listes
    public List<String> roomImages() {
        return roomImages != null ? roomImages : List.of();
    }

    public List<String> facilities() {
        return facilities != null ? facilities : List.of();
    }
}