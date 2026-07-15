package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response body of Travelport's Stays Hotel Availability endpoint
 * ({@code POST /hotel/availability/catalogofferingshospitality}), matching a verified real sample.
 * Each {@code CatalogOffering} is a bookable room/rate with a {@code Price} (PriceDetail carrying
 * Base/TotalTaxes/TotalFees/TotalPrice and a CurrencyCode object). The verified sample's mock did
 * not populate the room-type name/description inside {@code ProductOptions.Product}, so only the
 * offering id and price are relied upon here.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record TravelportHotelAvailabilityResponse(CatalogOfferingsHospitalityResponse CatalogOfferingsHospitalityResponse) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CatalogOfferingsHospitalityResponse(
            CatalogOfferings CatalogOfferings,
            String reservationStatus,
            TravelportSearchResponse.Result Result
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CatalogOfferings(String id, List<CatalogOffering> CatalogOffering) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CatalogOffering(String id, String CatalogOfferingRef, PriceDetail Price) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PriceDetail(
            CurrencyCode CurrencyCode,
            Double Base,
            Double TotalTaxes,
            Double TotalFees,
            Double TotalPrice
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CurrencyCode(String value) {
    }
}
