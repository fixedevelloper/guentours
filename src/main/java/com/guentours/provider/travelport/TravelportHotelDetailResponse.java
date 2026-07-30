package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response body of Travelport's Stays "Get Property Details" endpoint
 * ({@code GET /hotel/search/propertiesdetail?chainCode=...&propertyCode=...&ImageSize=Large}),
 * matching a verified real captured trace. Same {@code PropertiesResponse.Properties.PropertyInfo}
 * envelope as the property search response, but with a fuller {@code Property}: geo-coordinates,
 * photos, business/accessibility amenities and a room-count breakdown that the search response never
 * carries - kept as its own DTO rather than folded into {@link TravelportHotelSearchResponse} since
 * the two endpoints' {@code Property} shapes only partially overlap.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record TravelportHotelDetailResponse(PropertiesResponse PropertiesResponse) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PropertiesResponse(Properties Properties) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Properties(List<PropertyInfo> PropertyInfo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PropertyInfo(String id, Distance Distance, Property Property) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Distance(Double value, String unitOfDistance) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Property(
            String id,
            PropertyKey PropertyKey,
            String name,
            List<Rating> Rating,
            GeoLocation GeoLocation,
            List<Image> Image,
            List<BusinessService> BusinessService,
            List<AccessibilityFeature> AccessibilityFeature,
            List<GuestRoomInfo> GuestRoomInfo,
            Address Address,
            List<String> Telephone,
            Email Email,
            List<PropertyAmenity> PropertyAmenity
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PropertyKey(String chainCode, String propertyCode) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Rating(Double value, String provider) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeoLocation(Double latitude, Double longitude) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Image(String value, String dimensionCategory, String caption, Integer pictureCategory) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BusinessService(String code, String description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AccessibilityFeature(String value, String description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GuestRoomInfo(String code, Integer number, String description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Address(List<String> AddressLine, String City, StateProv StateProv, Country Country, String PostalCode) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StateProv(String value) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Country(String value, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Email(String value) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PropertyAmenity(String description, String code, String category) {
    }
}
