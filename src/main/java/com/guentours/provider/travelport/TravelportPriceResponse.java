package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response of Travelport's offer-pricing step
 * ({@code POST /price/offers/buildfromcatalogproductofferings}). Returns priced {@code Offer}s,
 * each with an {@code id} (the offer reference passed on to reservation creation) and a fresh
 * {@code Price}. Verify field names against a live sandbox response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record TravelportPriceResponse(OffersResponse OffersResponse) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OffersResponse(List<Offer> Offer) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Offer(String id, Price Price) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Price(Double TotalPrice, String CurrencyCode) {
    }
}
