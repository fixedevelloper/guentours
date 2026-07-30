package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response of Add Offer ({@code OfferListResponse}), matching a real production response: each
 * created offer carries an {@code Identifier.value} - not a flat {@code id}/{@code offerRef} as
 * first (incorrectly) assumed - used to reference it in later workbench steps.
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
    record Offer(@JsonProperty("@type") String type, Identifier Identifier) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Identifier(String value, String authority) {
    }
}
