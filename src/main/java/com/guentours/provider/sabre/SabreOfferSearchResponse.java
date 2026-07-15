package com.guentours.provider.sabre;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response body of Sabre's Bargain Finder Max v5 flight shopping endpoint
 * ({@code POST /v5/offers/shop}), matching a verified real response sample. Everything is
 * cross-referenced by numeric id: itineraries reference {@code legDescs} entries, legs
 * reference {@code scheduleDescs} entries (the actual flights). Schedule times are
 * time-of-day with a UTC offset (e.g. {@code "17:10:00+02:00"}) - the DATE comes from the
 * enclosing itinerary group's {@code groupDescription.legDescriptions}, not the schedule.
 * Seat availability and cabin live inside the pricing fare components, not the schedules.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record SabreOfferSearchResponse(GroupedItineraryResponse groupedItineraryResponse) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GroupedItineraryResponse(
            List<ScheduleDesc> scheduleDescs,
            List<LegDesc> legDescs,
            List<ItineraryGroup> itineraryGroups
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ScheduleDesc(
            int id,
            Integer stopCount,
            Boolean eTicketable,
            Integer elapsedTime,
            EndPoint departure,
            EndPoint arrival,
            Carrier carrier
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EndPoint(String airport, String city, String country, String time) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Carrier(String marketing, Integer marketingFlightNumber, String operating, Integer operatingFlightNumber) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LegDesc(int id, Integer elapsedTime, List<Ref> schedules) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Ref(int ref) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ItineraryGroup(GroupDescription groupDescription, List<Itinerary> itineraries) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GroupDescription(List<LegDescription> legDescriptions) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LegDescription(String departureDate, String departureLocation, String arrivalLocation) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Itinerary(int id, List<Ref> legs, List<PricingInformation> pricingInformation) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PricingInformation(Fare fare) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Fare(
            String validatingCarrierCode,
            Boolean eTicketable,
            String lastTicketDate,
            List<PassengerInfoWrapper> passengerInfoList,
            TotalFare totalFare
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PassengerInfoWrapper(PassengerInfo passengerInfo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PassengerInfo(String passengerType, Boolean nonRefundable, List<FareComponent> fareComponents) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FareComponent(Integer ref, String beginAirport, String endAirport, List<SegmentWrapper> segments) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SegmentWrapper(Segment segment) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Segment(String bookingCode, String cabinCode, Integer seatsAvailable) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TotalFare(
            double totalPrice,
            Double totalTaxAmount,
            String currency,
            Double equivalentAmount,
            String equivalentCurrency
    ) {
    }
}
