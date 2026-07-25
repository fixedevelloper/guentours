package com.guentours.provider.travelopro;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Response body from Travelopro's {@code /api/aeroVE5/availability} flight search endpoint. */
@JsonIgnoreProperties(ignoreUnknown = true)
record TraveloproAvailabilityResponse(AirSearchResponse AirSearchResponse) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AirSearchResponse(String session_id, String supplier, AirSearchResult AirSearchResult) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AirSearchResult(List<FareItineraryWrapper> FareItineraries) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FareItineraryWrapper(FareItinerary FareItinerary) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FareItinerary(
            AirItineraryFareInfo AirItineraryFareInfo,
            String DirectionInd,
            List<OriginDestinationOptionsEntry> OriginDestinationOptions,
            String ValidatingAirlineCode
    ) {}

    // IsRefundable, SplitItinerary et DivideInPartyIndicator volontairement absents :
    // Travelopro les sérialise tantôt en booléen JSON, tantôt en chaîne "Yes"/"No"/"false"
    // selon l'endpoint/l'itinéraire - un typage strict casse la désérialisation sur l'un
    // des deux formats. Comme aucun n'est lu par toFlightOffer, on les laisse à
    // ignoreUnknown plutôt que de risquer un type incompatible.
    @JsonIgnoreProperties(ignoreUnknown = true)
    record AirItineraryFareInfo(
            String FareSourceCode,
            String FareType,
            ItinTotalFares ItinTotalFares,
            List<FareBreakdownEntry> FareBreakdown
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ItinTotalFares(FareAmount BaseFare, FareAmount TotalTax, FareAmount TotalFare) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FareAmount(String Amount, String CurrencyCode) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FareBreakdownEntry(List<String> Baggage, PassengerTypeQuantity PassengerTypeQuantity) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PassengerTypeQuantity(String Code, Integer Quantity) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OriginDestinationOptionsEntry(List<SegmentWrapper> OriginDestinationOption, Integer TotalStops) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SegmentWrapper(FlightSegment FlightSegment, SeatsRemainingInfo SeatsRemaining) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FlightSegment(
            String MarketingAirlineCode,
            String MarketingAirlineName,
            String FlightNumber,
            String DepartureAirportLocationCode,
            String ArrivalAirportLocationCode,
            String DepartureDateTime,
            String ArrivalDateTime,
            String CabinClassCode,
            Integer JourneyDuration
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SeatsRemainingInfo(Boolean BelowMinimum, Integer Number) {}
}