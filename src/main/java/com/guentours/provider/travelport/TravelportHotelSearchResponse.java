package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response body of Travelport's Stays "Search Properties by Location" endpoint
 * ({@code POST /hotel/search/properties/search}), matching a verified real sample. Returns up to
 * 100 {@code PropertyInfo} entries, each wrapping a {@code Property} (name, key, rating, address,
 * geo) plus a {@code LowestAvailableRate}. Room/rate breakdown is not part of this response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record TravelportHotelSearchResponse(PropertiesResponse PropertiesResponse) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PropertiesResponse(Properties Properties) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Properties(int totalProperties, List<PropertyInfo> PropertyInfo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PropertyInfo(
            String id,
            String availability,
            Property Property,
            Amount LowestAvailableRate
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Property(
            String id,
            String name,
            PropertyKey PropertyKey,
            List<Rating> Rating,
            Address Address
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PropertyKey(String chainCode, String propertyCode) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Rating(Double value, String provider) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Address(String City, StateProv StateProv, Country Country) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StateProv(String value, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Country(String value, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Amount(Double value, String code) {
    }
}
