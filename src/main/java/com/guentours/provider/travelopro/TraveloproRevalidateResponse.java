package com.guentours.provider.travelopro;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record TraveloproRevalidateResponse(AirRevalidateResponse AirRevalidateResponse) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AirRevalidateResponse(AirRevalidateResult AirRevalidateResult) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AirRevalidateResult(Boolean IsValid, FareItineraries FareItineraries) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FareItineraries(FareItinerary FareItinerary) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FareItinerary(AirItineraryFareInfo AirItineraryFareInfo) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AirItineraryFareInfo(
            String FareSourceCode,
            String IsRefundable,
            ItinTotalFares ItinTotalFares,
            List<FareBreakdownEntry> FareBreakdown
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ItinTotalFares(MoneyAmount BaseFare, MoneyAmount TotalTax, MoneyAmount TotalFare) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MoneyAmount(String Amount, String CurrencyCode, Integer DecimalPlaces) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FareBreakdownEntry(List<String> Baggage, PassengerTypeQuantity PassengerTypeQuantity) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PassengerTypeQuantity(String Code, Integer Quantity) {}
}