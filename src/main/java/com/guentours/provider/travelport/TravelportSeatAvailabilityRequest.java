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
 * <p>The verified sample only populated {@code CustomerLoyalty}; {@code CatalogOfferingsIdentifier}/
 * {@code CatalogOfferingIdentifier}/{@code ProductIdentifier} below are this adapter's own addition,
 * modeled on the same {@code Ref}/{@code Identifier} shape {@link TravelportAncillaryOfferRequest}'s
 * verified Build Ancillary Offers request already uses to reference a catalog offering. Whether
 * they're the right shape is currently unconfirmed either way - live testing (2026-08-29) got a
 * generic account-entitlement 500 regardless of body content (see {@code TravelportClient
 * #querySeatAvailability}'s Javadoc), so this couldn't be validated against a real response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TravelportSeatAvailabilityRequest(
        @JsonProperty("@type") String type,
        SeatAvailabilityOfferings SeatAvailabilityOfferings
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record SeatAvailabilityOfferings(
            @JsonProperty("@type") String type,
            TravelportAncillaryOfferRequest.Ref CatalogOfferingsIdentifier,
            TravelportAncillaryOfferRequest.Ref CatalogOfferingIdentifier,
            TravelportAncillaryOfferRequest.Ref ProductIdentifier,
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
