package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response body of Travelport's Stays Hotel Availability endpoint
 * ({@code POST /hotel/availability/catalogofferingshospitality}), matching a verified real captured
 * trace. Each {@code CatalogOffering} is a bookable room/rate with a {@code Price} (PriceDetail
 * carrying Base/TotalTaxes/TotalFees/TotalPrice and a CurrencyCode object) and an
 * {@code Identifier{value,authority}} - an earlier, unverified version of this DTO guessed a flat
 * {@code CatalogOfferingRef} field instead, which the real trace does not carry. Room-type detail
 * lives at {@code ProductOptions[0].Product[0]} (bed configuration, room amenities, property
 * address/phone - the real trace always carries exactly one {@code ProductOptions} entry with
 * exactly one {@code Product}) and cancellation/guarantee/meal detail sits in a single
 * {@code TermsAndConditions} object (never an array, unlike {@code CancelPenalty}/{@code Guarantee}
 * within it, which are arrays even when there is only one entry). {@code CancelPenalty} is absent on
 * offerings that carry no explicit cancellation terms.
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
    record CatalogOfferings(
            int totalCatalogOffering,
            int catalogOfferingPerPage,
            int numberOfPages,
            List<CatalogOffering> CatalogOffering
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CatalogOffering(
            String id,
            Identifier Identifier,
            List<ProductOptions> ProductOptions,
            PriceDetail Price,
            TermsAndConditions TermsAndConditions
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Identifier(String value, String authority) {
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ProductOptions(List<Product> Product) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Product(
            String bookingCode,
            String propertyName,
            Address PropertyAddress,
            Telephone Telephone,
            PropertyKey PropertyKey,
            RoomType RoomType,
            DateRange DateRange
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Address(List<String> AddressLine, String City, StateProv StateProv, Country Country, String PostalCode) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StateProv(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Country(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Telephone(String phoneNumber, String cityCode) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PropertyKey(String chainCode, String propertyCode) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DateRange(String start, String end) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RoomType(RoomCharacteristics RoomCharacteristics, TextValue Description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RoomCharacteristics(
            String typeCode,
            String smokingAllowed,
            List<BedConfiguration> BedConfiguration,
            String accessibleRoom,
            List<RoomAmenity> RoomAmenity
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BedConfiguration(Integer quantity, String bedType, String size) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RoomAmenity(String description, Boolean includedInd, String code) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TextValue(String value) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TermsAndConditions(
            List<Guarantee> Guarantee,
            List<CancelPenalty> CancelPenalty,
            List<String> Description,
            String RatePaymentInfo,
            MealsIncluded MealsIncluded,
            DepositPolicy DepositPolicy,
            List<ProductRateCodeInfo> ProductRateCodeInfo
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Guarantee(String code, String guaranteeType) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CancelPenalty(String Description, Deadline Deadline, HotelPenalty HotelPenalty, String Refundable) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Deadline(SpecificDate SpecificDate, String Time) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpecificDate(String start, String end) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record HotelPenalty(String appliesTo, Double Percent) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MealsIncluded(Boolean breakfastInd) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DepositPolicy(List<DepositAmount> Deposit) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DepositAmount(CurrencyAmount CurrencyAmount) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CurrencyAmount(Double value, String code) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ProductRateCodeInfo(RateCodeInfo RateCodeInfo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RateCodeInfo(String rateCategory) {
    }
}
