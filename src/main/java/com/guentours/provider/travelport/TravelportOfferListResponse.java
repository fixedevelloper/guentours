package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response of Travelport's build-ancillary-offers endpoint ({@code OfferListResponse}). Returns
 * the created {@code Offer}(s) - each with an {@code id}/{@code offerRef} used to add the ancillary
 * or paid seat to the workbench in the following step. Verify against a real response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record TravelportOfferListResponse(OfferListResponse OfferListResponse) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OfferListResponse(
            String transactionId,
            List<Offer> OfferID,
            TravelportSearchResponse.Result Result
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Offer(String id, String offerRef, String ContentSource) {
    }
}
