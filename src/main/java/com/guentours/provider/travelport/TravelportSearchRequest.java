package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request body for Travelport's JSON air-shopping endpoint
 * ({@code POST /air/catalog/search/catalogproductofferings}), matching a verified real request:
 * the top-level object itself carries {@code @type: CatalogProductOfferingsQueryRequest} (it is
 * not wrapped in a field of that name), with the actual query under
 * {@code CatalogProductOfferingsRequest}. One {@code SearchCriteriaFlight} entry per leg
 * (two for a round trip), a {@code PassengerCriteria} per passenger type, and an optional
 * {@code SearchModifiersAir} for carrier preferences.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TravelportSearchRequest(
        @JsonProperty("@type") String type,
        CatalogProductOfferingsRequest CatalogProductOfferingsRequest
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record CatalogProductOfferingsRequest(
            @JsonProperty("@type") String type,
            Integer maxNumberOfUpsellsToReturn,
            Integer offersPerPage,
            List<String> contentSourceList,
            List<PassengerCriteria> PassengerCriteria,
            List<SearchCriteriaFlight> SearchCriteriaFlight,
            SearchModifiersAir SearchModifiersAir
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PassengerCriteria(
            @JsonProperty("@type") String type,
            int number,
            Integer age,
            String passengerTypeCode
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record SearchCriteriaFlight(
            @JsonProperty("@type") String type,
            String departureDate,
            Endpoint From,
            Endpoint To
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Endpoint(String value) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record SearchModifiersAir(
            @JsonProperty("@type") String type,
            List<CarrierPreference> CarrierPreference
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record CarrierPreference(
            @JsonProperty("@type") String type,
            String preferenceType,
            List<String> carriers
    ) {
    }
}
