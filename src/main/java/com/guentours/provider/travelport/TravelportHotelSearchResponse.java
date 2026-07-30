package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response body of Travelport's Stays "Search Properties by Location" endpoint
 * ({@code POST /hotel/search/properties/search}), matching a verified real captured trace. Returns
 * up to 100 {@code PropertyInfo} entries per page ({@code numberOfPages} of {@code propertiesPerPage}
 * each), every entry wrapping a {@code Property} (name, key, rating, address, geo) plus a
 * {@code LowestAvailableRate}/{@code MaximumAvailableRate}. A property with no rooms available for
 * the stay (e.g. {@code availability: "Close"}) carries neither rate. Room/rate breakdown detail is
 * not part of this response - it comes from a follow-up availability step. {@code Property} also
 * carries an {@code Image} array (same shape as the detail endpoint's), used here as a source of
 * cover photos for search results.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record TravelportHotelSearchResponse(PropertiesResponse PropertiesResponse) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PropertiesResponse(Properties Properties) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Properties(
            int totalProperties,
            int propertiesPerPage,
            int numberOfPages,
            List<PropertyInfo> PropertyInfo
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PropertyInfo(
            String id,
            Identifier Identifier,
            String availability,
            Distance Distance,
            Property Property,
            Amount LowestAvailableRate,
            Amount MaximumAvailableRate
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Identifier(String value, String authority) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Distance(Double value, String unitOfDistance) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Property(
            String id,
            String name,
            PropertyKey PropertyKey,
            List<Rating> Rating,
            Address Address,
            List<Image> Image
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Image(String value, String dimensionCategory, String caption, Integer pictureCategory) {
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
