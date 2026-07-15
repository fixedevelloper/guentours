package com.guentours.provider.sabre;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Request body for Sabre's Bargain Finder Max v5 flight shopping endpoint
 * ({@code POST /v5/offers/shop}), matching a verified working request sample:
 * {@code POS.Source} carries a {@code RequestorID} (Type "1", CompanyName "TN"),
 * {@code DepartureDateTime} is a full date-time, and the number of itineraries
 * wanted is requested via {@code TPA_Extensions.IntelliSellTransaction.RequestType}
 * (e.g. "50ITINS").
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record SabreOfferSearchRequest(OTA_AirLowFareSearchRQ OTA_AirLowFareSearchRQ) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record OTA_AirLowFareSearchRQ(
            String Version,
            POS POS,
            List<OriginDestinationInformation> OriginDestinationInformation,
            TravelPreferences TravelPreferences,
            TravelerInfoSummary TravelerInfoSummary,
            TPA_Extensions TPA_Extensions
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record POS(List<Source> Source) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Source(String PseudoCityCode, RequestorID RequestorID) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record RequestorID(String Type, String ID, CompanyName CompanyName) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record CompanyName(String Code) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record OriginDestinationInformation(
            String DepartureDateTime,
            Location OriginLocation,
            Location DestinationLocation,
            OriginDestinationTpaExtensions TPA_Extensions
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Location(String LocationCode) {
    }

    /** Per-OD extensions: pins the exact flight segments when revalidating a specific itinerary. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record OriginDestinationTpaExtensions(List<Flight> Flight) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Flight(
            Integer Number,
            String DepartureDateTime,
            String ArrivalDateTime,
            String Type,
            Location OriginLocation,
            Location DestinationLocation,
            Airline Airline
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Airline(String Marketing, String Operating) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record TravelPreferences(List<CabinPref> CabinPref) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record CabinPref(String Cabin, String PreferLevel) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record TravelerInfoSummary(
            List<AirTravelerAvail> AirTravelerAvail,
            PriceRequestInformation PriceRequestInformation
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record AirTravelerAvail(List<PassengerTypeQuantity> PassengerTypeQuantity) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PassengerTypeQuantity(String Code, int Quantity) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PriceRequestInformation(String CurrencyCode) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record TPA_Extensions(IntelliSellTransaction IntelliSellTransaction) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record IntelliSellTransaction(RequestType RequestType) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record RequestType(String Name) {
    }
}
