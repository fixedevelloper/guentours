package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request body for Travelport's build-ancillary-offers endpoint
 * ({@code POST /air/book/airoffer/reservationworkbench/{sessionId}/offers/buildancillaryoffersfromcatalogofferings}),
 * matching a verified real request ({@code @type: OfferQueryBuildAncillaryOffersFromCatalogOfferings}).
 * It runs inside a workbench session (the session id is a path segment AND the
 * {@code travelportPlusSessionIdentifier} header) and turns a chosen ancillary/paid-seat catalog
 * offering into an offer that can then be added to the workbench.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TravelportAncillaryOfferRequest(
        @JsonProperty("@type") String type,
        List<BuildAncillaryOffersFromCatalogOfferings> BuildAncillaryOffersFromCatalogOfferings
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record BuildAncillaryOffersFromCatalogOfferings(
            @JsonProperty("@type") String type,
            Ref CatalogOfferingsIdentifier,
            Ref CatalogOfferingIdentifier,
            Ref ProductIdentifier,
            Integer Quantity
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Ref(String id, Identifier Identifier) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Identifier(String value, String authority) {
    }
}
