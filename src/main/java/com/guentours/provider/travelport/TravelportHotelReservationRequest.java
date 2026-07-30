package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request body for Travelport's Stays Create Hotel Reservation (full payload) endpoint
 * ({@code POST /hotel/book/reservations}), matching a verified real captured trace. Its
 * {@code ReservationDetail} is its own shape, not the flights workbench's {@code Reservation} an
 * earlier, unverified version of this DTO reused: the {@code Offer} carries a Stays
 * {@code ProductHospitality} (bookingCode, PropertyKey, DateRange) plus {@code Price} and rate-code
 * detail, and a {@code FormOfPayment}/{@code Payment} pair is required here even though the actual
 * customer payment is collected separately through our own PaymentGateway - Travelport requires a
 * guarantee card on every hotel reservation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TravelportHotelReservationRequest(ReservationDetail ReservationDetail) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ReservationDetail(
            List<Offer> Offer,
            List<Payment> Payment,
            List<FormOfPayment> FormOfPayment,
            List<Traveler> Traveler
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Offer(
            @JsonProperty("@type") String type,
            Identifier Identifier,
            List<Product> Product,
            PriceDetail Price,
            List<TermsAndConditionsFull> TermsAndConditionsFull
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Identifier(String authority) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Product(
            @JsonProperty("@type") String type,
            String bookingCode,
            String Quantity,
            Integer guests,
            PropertyKey PropertyKey,
            DateRange DateRange
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PropertyKey(@JsonProperty("@type") String type, String propertyCode, String chainCode) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record DateRange(String start, String end) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PriceDetail(
            @JsonProperty("@type") String type,
            CurrencyCode CurrencyCode,
            Double Base,
            Double TotalTaxes,
            Double TotalPrice
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record CurrencyCode(String value) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record TermsAndConditionsFull(List<ProductRateCodeInfo> ProductRateCodeInfo) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ProductRateCodeInfo(@JsonProperty("@type") String type, RateCodeInfo RateCodeInfo) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record RateCodeInfo(String rateID, String value) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Payment(@JsonProperty("@type") String type, Amount Amount, Boolean guaranteeInd, Boolean depositInd) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Amount(String code, Double value) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record FormOfPayment(@JsonProperty("@type") String type, PaymentCard PaymentCard) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PaymentCard(
            @JsonProperty("@type") String type,
            String expireDate,
            String CardType,
            String CardCode,
            String CardHolderName,
            CardNumber CardNumber,
            SeriesCode SeriesCode,
            PaymentCardPersonName PersonName,
            Address Address,
            List<Telephone> Telephone,
            List<Email> Email
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record CardNumber(@JsonProperty("@type") String type, String PlainText) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record SeriesCode(@JsonProperty("@type") String type, String PlainText) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PaymentCardPersonName(
            @JsonProperty("@type") String type, String Given, String Middle, String Surname
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Address(
            @JsonProperty("@type") String type,
            AddressNumber Number,
            String Street,
            List<String> AddressLine,
            String City,
            String County,
            StateProv StateProv,
            Country Country,
            String PostalCode
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record AddressNumber(String value) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record StateProv(String value, String name) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Country(String value, String name) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Telephone(
            @JsonProperty("@type") String type,
            String countryAccessCode,
            String areaCityCode,
            String phoneNumber,
            String cityCode
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Email(String value) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Traveler(
            @JsonProperty("@type") String type,
            TravelerPersonName PersonName,
            List<Telephone> Telephone,
            List<Email> Email
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record TravelerPersonName(
            @JsonProperty("@type") String type, String Given, String Surname, String Title
    ) {
    }
}
