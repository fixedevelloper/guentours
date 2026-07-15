package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request body for Travelport's Stays Hotel Availability endpoint
 * ({@code POST /hotel/availability/catalogofferingshospitality}), matching a verified real sample.
 * Returns room types and rates for one or more specified properties on the stay dates. The property
 * is identified by its {@code PropertyKey} (chain code + property code) captured from the search.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TravelportHotelAvailabilityRequest(CatalogOfferingsQueryRequest CatalogOfferingsQueryRequest) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record CatalogOfferingsQueryRequest(
            @JsonProperty("@type") String type,
            List<CatalogOfferingsRequest> CatalogOfferingsRequest
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record CatalogOfferingsRequest(
            @JsonProperty("@type") String type,
            String requestedCurrency,
            StayDates StayDates,
            HotelSearchCriterion HotelSearchCriterion
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record StayDates(String start, String end) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record HotelSearchCriterion(
            @JsonProperty("@type") String type,
            int numberOfRooms,
            List<PropertyRequest> PropertyRequest
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PropertyRequest(
            @JsonProperty("@type") String type,
            PropertyKey PropertyKey
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PropertyKey(String chainCode, String propertyCode) {
    }
}
