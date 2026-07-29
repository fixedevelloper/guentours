package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * Add Offer <b>reference payload</b> request
 * ({@code POST /air/book/airoffer/reservationworkbench/{sessionId}/offers/buildfromcatalogproductofferings}),
 * matching a real production reference client for this exact endpoint. Unlike the pricing step
 * ({@link TravelportPriceRequest}), the whole request body is wrapped under a top-level field named
 * after the {@code @type} itself ({@code OfferQueryBuildFromCatalogProductOfferings}), and carries
 * its own simpler {@code PaymentCriteria} (just the four commercial-model booleans, no card/issuer
 * fields) plus a {@code ProductBrandOfferingIdentifier} and {@code ProductIdentifier} per selection.
 *
 * <p>The reference threads the offering id and the CatalogProductOfferings container's own
 * Identifier value through every reference field (offering id, brand identifier, product
 * identifier) when it doesn't have finer-grained ids for this step - we do the same here, reusing
 * whatever richer identifiers we captured at search time ({@code productRef}) when available.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TravelportAddOfferRequest(OfferQueryBuildFromCatalogProductOfferings OfferQueryBuildFromCatalogProductOfferings) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"@type", "PaymentCriteria", "BuildFromCatalogProductOfferingsRequest", "MaxNumberOfUpsellsToReturn"})
    record OfferQueryBuildFromCatalogProductOfferings(
            @JsonProperty("@type") String type,
            PaymentCriteria PaymentCriteria,
            BuildFromCatalogProductOfferingsRequest BuildFromCatalogProductOfferingsRequest,
            Integer MaxNumberOfUpsellsToReturn
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"@type", "agencyAccountInd", "bspInd", "cashInd", "invoiceInd"})
    record PaymentCriteria(
            @JsonProperty("@type") String type,
            boolean agencyAccountInd,
            boolean bspInd,
            boolean cashInd,
            boolean invoiceInd
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"@type", "CatalogProductOfferingsIdentifier", "CatalogProductOfferingSelection"})
    record BuildFromCatalogProductOfferingsRequest(
            @JsonProperty("@type") String type,
            OfferingsRef CatalogProductOfferingsIdentifier,
            List<CatalogProductOfferingSelection> CatalogProductOfferingSelection
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"@type", "CatalogProductOfferingIdentifier", "ProductBrandOfferingIdentifier",
            "ProductIdentifier", "SegmentSequence"})
    record CatalogProductOfferingSelection(
            @JsonProperty("@type") String type,
            OfferingRef CatalogProductOfferingIdentifier,
            Identifier ProductBrandOfferingIdentifier,
            List<ProductIdentifier> ProductIdentifier,
            List<Integer> SegmentSequence
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"id", "Identifier"})
    record OfferingsRef(String id, Identifier Identifier) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"id", "Identifier", "CatalogProductOfferingRef"})
    record OfferingRef(String id, Identifier Identifier, String CatalogProductOfferingRef) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"id", "productRef", "Identifier"})
    record ProductIdentifier(String id, String productRef, Identifier Identifier) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Identifier(String value, String authority) {
    }
}
