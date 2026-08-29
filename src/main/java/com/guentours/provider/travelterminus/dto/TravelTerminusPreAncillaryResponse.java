package com.guentours.provider.travelterminus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@code data} of {@code POST /api/flights/pre-ancillary}: baggage/meal/seat extras grouped by
 * leg or segment (see the "Core Logic Mapping" doc section), each carrying a nested {@code
 * passengers[]} array with per-passenger eligibility/pricing. Every passenger entry's {@code
 * flightObject} is an opaque token ({@code offerId}+{@code paxRef}) that must be passed back
 * unmodified inside that passenger's {@code baggages[]}/{@code meals[]}/{@code seats[]} array in
 * the Book request - never read or reconstruct its fields.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TravelTerminusPreAncillaryResponse(
        String status,
        List<BaggageGroup> baggages,
        List<MealGroup> meals,
        List<SeatGroup> seats
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaxPrice(String paxType, String currency, String currencySymbol, BigDecimal price, JsonNode flightObject) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BaggageGroup(String segmentId, String departure, String arrival, List<BagOption> bagsData) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BagOption(Integer baggagePiece, String baggageWeight, List<PaxPrice> passengers) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MealGroup(String segmentId, String departure, String arrival, List<MealOption> mealsData) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MealOption(String code, String description, BigDecimal price, String currency, List<PaxPrice> passengers) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeatGroup(String segmentId, String airlineCode, String flightNum, String departure, String arrival,
                             String cabinClass, Integer totalRows, Integer totalColumns, List<String> seatGroups,
                             List<SeatOption> seatData) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeatOption(String row, String column, String status, Boolean exitRow, Boolean accessible,
                              Boolean bassinet, Boolean toilet, Boolean galley, List<PaxPrice> passengers) {
    }
}
