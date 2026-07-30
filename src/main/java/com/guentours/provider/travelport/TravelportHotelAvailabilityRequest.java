package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request body for Travelport's Stays Hotel Availability endpoint
 * ({@code POST /hotel/availability/catalogofferingshospitality}), matching a verified real captured
 * trace. Returns room types and rates for one or more specified properties on the stay dates. The
 * property is identified by its {@code PropertyKey} (chain code + property code) captured from the
 * search. The real trace's outer {@code CatalogOfferingsQueryRequest} wrapper carries no
 * {@code @type} of its own (only the inner {@code CatalogOfferingsRequest} does), and
 * {@code HotelSearchCriterion} also needs a {@code RoomStayCandidates} block (guest counts) alongside
 * {@code numberOfRooms} - both absent from an earlier, unverified version of this DTO.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TravelportHotelAvailabilityRequest(CatalogOfferingsQueryRequest CatalogOfferingsQueryRequest) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record CatalogOfferingsQueryRequest(
            List<CatalogOfferingsRequest> CatalogOfferingsRequest
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record CatalogOfferingsRequest(
            @JsonProperty("@type") String type,
            Boolean verboseResponseInd,
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
            List<PropertyRequest> PropertyRequest,
            RoomStayCandidates RoomStayCandidates
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PropertyRequest(
            @JsonProperty("@type") String type,
            PropertyKey PropertyKey
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PropertyKey(@JsonProperty("@type") String type, String chainCode, String propertyCode) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record RoomStayCandidates(
            @JsonProperty("@type") String type,
            List<RoomStayCandidate> RoomStayCandidate
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record RoomStayCandidate(@JsonProperty("@type") String type, GuestCounts GuestCounts) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record GuestCounts(@JsonProperty("@type") String type, List<GuestCount> GuestCount) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record GuestCount(@JsonProperty("@type") String type, int count) {
    }
}
