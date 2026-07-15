package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request body for Travelport's offer-pricing step
 * ({@code POST /price/offers/buildfromcatalogproductofferings}), used to re-price/validate a
 * chosen search offering before creating a reservation - the equivalent of a revalidation.
 *
 * <p>Travelport normally prices by re-selecting an offering within the SAME shopping session
 * (via its offerings identifier), but our canonical {@link com.guentours.provider.FlightOffer}
 * only keeps the offering id, so this re-prices by that id alone. Verify against a live sandbox:
 * a fresh price call may require the original search's offerings identifier and product ids.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TravelportPriceRequest(PriceProductsQueryRequest PriceProductsQueryRequest) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PriceProductsQueryRequest(
            @JsonProperty("@type") String type,
            List<CatalogProductOfferingSelection> CatalogProductOfferingSelection
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record CatalogProductOfferingSelection(
            @JsonProperty("@type") String type,
            Identifier CatalogProductOfferingIdentifier
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Identifier(String value) {
    }
}
