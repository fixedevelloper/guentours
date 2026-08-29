package com.guentours.provider.travelterminus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/** {@code data} of {@code GET /api/flights/order-details/{bookingRefId}}. Despite the field
 *  reference table in the docs calling {@code pnr}/{@code flightTicketNo} string arrays, the
 *  actual captured sample response returns them as a single, possibly {@code " | "}-delimited
 *  string (one segment per leg/carrier) - modeled as {@code String} here to match the real
 *  payload, split on {@code "|"} where a list is needed.
 *
 *  <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} at every level here: fields this app
 *  doesn't currently surface (route/flightSegments, searchReqId, ...) are simply dropped rather
 *  than declared, since Booking already stores the route/times from our own search-time offer. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TravelTerminusOrderDetailsResponse(
        String status,
        String bookingRefId,
        String bookingStatus,
        String bookingDate,
        String airTripType,
        String travelDate,
        String departure,
        String arrival,
        String pnr,
        String flightTicketNo,
        String totalAmount,
        String currencyCode,
        List<Pax> paxes,
        List<BookingTraveler> bookingTravelers,
        FareRules fareRules
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Pax(String type, Integer quantity) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BookingTraveler(
            String bookingTravelerId,
            String title,
            String firstName,
            String lastName,
            String paxType,
            List<BaggageGroup> baggages,
            List<MealGroup> meals,
            List<SeatGroup> seats
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BaggageGroup(String segmentId, String departure, String arrival, List<BagData> bagsData) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BagData(String baggageWeight, BigDecimal price, BigDecimal tax, String paxRef, String currency) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MealGroup(String segmentId, String departure, String arrival, List<MealData> mealsData) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MealData(String description, BigDecimal price, BigDecimal tax, String paxRef, String currency) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeatGroup(String segmentId, String departure, String arrival, List<SeatData> seatData) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SeatData(String row, String column, BigDecimal price, BigDecimal tax, String paxRef, String currency) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FareRules(List<CancellationRule> cancellation) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CancellationRule(BigDecimal adultCharges, String currency, Boolean refundable) {
    }
}
