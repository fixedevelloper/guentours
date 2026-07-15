package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Add Offer <b>reference payload</b> request
 * ({@code POST /air/book/airoffer/reservationworkbench/{sessionId}/offers/buildfromcatalogproductofferings}),
 * matching a verified real sample ({@code @type: OfferQueryBuildFromCatalogProductOfferings}). It
 * adds a searched offer to the workbench by sending identifiers from the Search response rather
 * than full itinerary details - the reference payload is the only form NDC supports. Requires the
 * initial Search to have sent {@code offersPerPage} so its results were cached.
 *
 * <p>The reference payload references the search's {@code CatalogProductOfferings} container id and
 * the chosen offering id (and optionally the ProductBrandOffering/Product identifiers and segment
 * sequence). Our canonical {@link com.guentours.provider.FlightOffer} only retains the offering id,
 * so those richer identifiers must be threaded through from the Search response for a fully correct
 * reference payload; this sends the offering id in both required positions as a best effort.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TravelportAddOfferRequest(
        @JsonProperty("@type") String type,
        BuildFromCatalogProductOfferingsRequest BuildFromCatalogProductOfferingsRequest,
        Integer MaxNumberOfUpsellsToReturn
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record BuildFromCatalogProductOfferingsRequest(
            @JsonProperty("@type") String type,
            OfferingsRef CatalogProductOfferingsIdentifier,
            List<CatalogProductOfferingSelection> CatalogProductOfferingSelection
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record CatalogProductOfferingSelection(
            @JsonProperty("@type") String type,
            OfferingRef CatalogProductOfferingIdentifier,
            List<Integer> SegmentSequence
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record OfferingsRef(String id, Identifier Identifier) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record OfferingRef(String id, Identifier Identifier, String CatalogProductOfferingRef) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Identifier(String value, String authority) {
    }
}
