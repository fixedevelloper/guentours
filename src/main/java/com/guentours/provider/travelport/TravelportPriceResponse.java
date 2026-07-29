package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response of Travelport's offer-pricing step
 * ({@code POST /air/price/offers/buildfromcatalogproductofferings}). A real (error) response from
 * this exact endpoint confirmed the top-level wrapper is {@code OfferListResponse} - the same
 * "build offers" envelope {@link TravelportOfferListResponse} already uses for the Add Offer step
 * - not the invented {@code OffersResponse} this previously guessed. A real priced response
 * confirmed the per-offer field really is {@code Price} (not {@code BestCombinablePrice} like the
 * search response), but its {@code CurrencyCode} is the same nested {@code {"value": "TRY",
 * "decimalPlace": 2}} object the search response uses, not a flat string - reusing
 * {@link TravelportSearchResponse.CurrencyCode} rather than duplicating that record.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record TravelportPriceResponse(OfferListResponse OfferListResponse) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OfferListResponse(String transactionId, List<Offer> OfferID, TravelportSearchResponse.Result Result) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Offer(String id, String offerRef, String ContentSource, Price Price) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Price(Double TotalPrice, TravelportSearchResponse.CurrencyCode CurrencyCode) {
    }
}
