package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * Request body for Travelport's offer-pricing step
 * ({@code POST /air/price/offers/buildfromcatalogproductofferings}).
 *
 * <p>Matches a real, working reference implementation for this exact endpoint (a production PHP
 * client), which is richer than the vendor's own minimal DevKit sample: every offering/product
 * reference carries both a flat {@code id} and a nested {@code Identifier{value, authority}} (the
 * DevKit sample's Identifier-only shape turned out to be incomplete), plus a
 * {@code ProductBrandOfferingIdentifier} (flat {@code value}/{@code authority}, reusing the
 * CatalogProductOfferings container's own identifier value), a {@code PassengerCriteria} list, a
 * {@code FareRuleType: "Structured"}, and a top-level {@code PaymentCriteria} block that the
 * reference always sends with fixed generic values (not tied to a real card) to price across all
 * commercial models.
 *
 * <p>The reference builds one {@code CatalogProductOfferingSelection} per itinerary leg for
 * multi-segment trips; our canonical {@link com.guentours.provider.FlightOffer} only keeps one
 * leg's identifiers today, so this sends a single selection. {@code PassengerCriteria} here is a
 * single default ADT - {@link com.guentours.provider.TravelProviderClient#verifyFlightPrice} only
 * receives the offer, not the traveler mix chosen at checkout, so the real passenger composition
 * isn't available at this point without a wider interface change.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"@type", "BuildFromCatalogProductOfferingsRequest", "PaymentCriteria", "MaxNumberOfUpsellsToReturn"})
record TravelportPriceRequest(
        @JsonProperty("@type") String type,
        BuildFromCatalogProductOfferingsRequest BuildFromCatalogProductOfferingsRequest,
        PaymentCriteria PaymentCriteria,
        Integer MaxNumberOfUpsellsToReturn
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"@type", "CatalogProductOfferingsIdentifier", "CatalogProductOfferingSelection",
            "PassengerCriteria", "FareRuleType"})
    record BuildFromCatalogProductOfferingsRequest(
            @JsonProperty("@type") String type,
            OfferingsRef CatalogProductOfferingsIdentifier,
            List<CatalogProductOfferingSelection> CatalogProductOfferingSelection,
            List<PassengerCriteria> PassengerCriteria,
            String FareRuleType
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"@type", "number", "passengerTypeCode", "id"})
    record PassengerCriteria(
            @JsonProperty("@type") String type,
            int number,
            String passengerTypeCode,
            String id
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"@type", "IssuerIdentificationNumber", "PaymentCardCode", "agencyAccountInd", "bspInd",
            "cashInd", "invoiceInd"})
    record PaymentCriteria(
            @JsonProperty("@type") String type,
            String IssuerIdentificationNumber,
            String PaymentCardCode,
            boolean agencyAccountInd,
            boolean bspInd,
            boolean cashInd,
            boolean invoiceInd
    ) {
    }
}
