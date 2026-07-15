package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response envelope of Travelport's Seat Map / ancillary-list endpoint
 * ({@code CatalogOfferingsAncillaryListResponse}). The verified sample was a mock whose
 * {@code CatalogOffering} entries carried only pricing ({@code PriceDetail}) - the actual seat
 * inventory grid (rows, columns, per-seat availability and paid/free status) was NOT present in
 * the sample, so it is not modelled here yet. This captures the parseable envelope (offerings +
 * price + result) pending a real seat-level response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record TravelportAncillaryListResponse(CatalogOfferingsAncillaryListResponse CatalogOfferingsAncillaryListResponse) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CatalogOfferingsAncillaryListResponse(
            String transactionId,
            List<CatalogOfferings> CatalogOfferingsID,
            TravelportSearchResponse.Result Result
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CatalogOfferings(String id, List<CatalogOffering> CatalogOffering) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CatalogOffering(String id, String ContentSource, PriceDetail Price) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PriceDetail(
            @JsonProperty("@type") String type,
            CurrencyCode CurrencyCode,
            Double Base,
            Double TotalTaxes,
            Double TotalFees,
            Double TotalPrice
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CurrencyCode(String value) {
    }
}
