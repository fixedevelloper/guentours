package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * Add Offer <b>reference payload</b> request
 * ({@code POST /air/book/airoffer/reservationworkbench/{sessionId}/offers/buildfromcatalogproductofferings}),
 * matching a real captured Travelport DevKit Postman trace for this exact endpoint (request and its
 * successful response verified together). This is deliberately minimal - an earlier, unverified
 * attempt at this shape added a {@code PaymentCriteria}, a {@code MaxNumberOfUpsellsToReturn}, a
 * {@code ProductBrandOfferingIdentifier}, a per-selection {@code @type}, a {@code SegmentSequence},
 * and an {@code authority} on every identifier - none of those appear in the verified trace, and
 * every one of the ids the earlier shape carried on {@code OfferingsRef}/{@code OfferingRef}/
 * {@code ProductIdentifier} turned out to not exist either: those three are each just a bare
 * {@code Identifier} wrapper carrying a single {@code value}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TravelportAddOfferRequest(OfferQueryBuildFromCatalogProductOfferings OfferQueryBuildFromCatalogProductOfferings) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"@type", "BuildFromCatalogProductOfferingsRequest"})
    record OfferQueryBuildFromCatalogProductOfferings(
            @JsonProperty("@type") String type,
            BuildFromCatalogProductOfferingsRequest BuildFromCatalogProductOfferingsRequest
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"@type", "CatalogProductOfferingsIdentifier", "CatalogProductOfferingSelection"})
    record BuildFromCatalogProductOfferingsRequest(
            @JsonProperty("@type") String type,
            IdentifierRef CatalogProductOfferingsIdentifier,
            List<CatalogProductOfferingSelection> CatalogProductOfferingSelection
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"CatalogProductOfferingIdentifier", "ProductIdentifier"})
    record CatalogProductOfferingSelection(
            IdentifierRef CatalogProductOfferingIdentifier,
            List<IdentifierRef> ProductIdentifier
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record IdentifierRef(Identifier Identifier) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Identifier(String value) {
    }
}
