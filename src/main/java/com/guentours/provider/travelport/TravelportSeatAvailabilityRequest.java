package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request body for Travelport's Seat Map endpoint
 * ({@code POST /air/search/seat/catalogofferingsancillaries/seatavailabilities}), matching a
 * verified real request ({@code @type: CatalogOfferingsQuerySeatAvailability}). This is the
 * "reference payload" form: it is sent inside a workbench session (the session id goes in the
 * {@code travelportPlusSessionIdentifier} header) and references a flight/offer/product already
 * searched, priced or booked - so paid seats require a workbench to have been created first.
 *
 * <p>The verified sample only populated {@code CustomerLoyalty}; the offering/product/flight
 * reference identifiers that scope the seat map to a specific itinerary are documented but were
 * not shown, so those still need confirming against a real request.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TravelportSeatAvailabilityRequest(
        @JsonProperty("@type") String type,
        SeatAvailabilityOfferings SeatAvailabilityOfferings
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record SeatAvailabilityOfferings(
            @JsonProperty("@type") String type,
            List<CustomerLoyalty> CustomerLoyalty
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record CustomerLoyalty(
            String value,
            String id,
            String programId,
            String supplier,
            String tier,
            String cardHolderName
    ) {
    }
}
