package com.guentours.provider.travelopro;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Response body from Travelopro's {@code /api/aeroVE5/availability} flight search endpoint. */
@JsonIgnoreProperties(ignoreUnknown = true)
record TraveloproAvailabilityResponse(AirSearchResponse AirSearchResponse) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AirSearchResponse(String session_id, AirSearchResult AirSearchResult) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AirSearchResult(List<FareItineraryWrapper> FareItineraries) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FareItineraryWrapper(FareItinerary FareItinerary) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FareItinerary(
            AirItineraryFareInfo AirItineraryFareInfo,
            String DirectionInd,
            List<OriginDestinationOptionsEntry> OriginDestinationOptions,
            String ValidatingAirlineCode
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AirItineraryFareInfo(String FareSourceCode, ItinTotalFares ItinTotalFares) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ItinTotalFares(FareAmount TotalFare) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FareAmount(String Amount, String CurrencyCode) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OriginDestinationOptionsEntry(List<SegmentWrapper> OriginDestinationOption, Integer TotalStops) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SegmentWrapper(FlightSegment FlightSegment, SeatsRemaining SeatsRemaining) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FlightSegment(
            String MarketingAirlineCode,
            String FlightNumber,
            String DepartureAirportLocationCode,
            String ArrivalAirportLocationCode,
            String DepartureDateTime,
            String ArrivalDateTime,
            String CabinClassCode
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SeatsRemaining(Integer Number) {
    }
}
