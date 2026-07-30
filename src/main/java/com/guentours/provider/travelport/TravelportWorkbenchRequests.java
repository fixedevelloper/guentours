package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * Request bodies for Travelport's workbench/session-based booking workflow (its "JSON APIs
 * Required Full Workflow"). The New Workbench step ({@code POST /air/book/session/reservationworkbench})
 * takes a {@code @type: Reservation} body and is aligned with a verified real sample: it can carry
 * the {@code Offer}(s) and {@code Traveler}(s) directly (the allowed full-payload form), so create
 * workbench + add offer + add travelers are combined into this one call. {@code FormOfPayment} and
 * {@code Payment} are also part of the Reservation body, used at ticketing time.
 *
 * <p>Every workbench call threads one client-supplied {@code travelportPlusSessionIdentifier}
 * through the header. Only the fields we populate are modelled; the commit and post-commit
 * endpoints/payloads still need verifying against real samples.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
final class TravelportWorkbenchRequests {

    private TravelportWorkbenchRequests() {
    }

    /**
     * New Workbench body ({@code @type: ReservationID}) - a verified real production reference
     * client sends only this bare type marker to have Travelport allocate a fresh workbench and
     * return its id; it does not carry the offer/traveler/payment payload up front.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ReservationId(@JsonProperty("@type") String type) {
    }

    /** Full Reservation body ({@code @type: Reservation}) - the container committed into a PNR. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"@type", "Offer", "Traveler", "FormOfPayment", "Payment"})
    record Reservation(
            @JsonProperty("@type") String type,
            List<Offer> Offer,
            List<Traveler> Traveler,
            List<FormOfPayment> FormOfPayment,
            List<Payment> Payment
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"@type", "id", "offerRef", "Identifier", "ContentSource"})
    record Offer(
            @JsonProperty("@type") String type,
            String id,
            String offerRef,
            Identifier Identifier,
            String ContentSource
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Identifier(String value, String authority) {
    }

    /**
     * Matches a real production reference client's Add Traveler payload: {@code gender} and an
     * {@code id} (e.g. {@code trav_1}) alongside the fields already modelled, plus a
     * {@code Telephone} entry and a {@code TravelDocument} (passport) entry - both absent from the
     * earlier, minimal version of this DTO.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"@type", "gender", "birthDate", "id", "passengerTypeCode", "PersonName", "Telephone",
            "Email", "TravelDocument"})
    record Traveler(
            @JsonProperty("@type") String type,
            String gender,
            String birthDate,
            String id,
            String passengerTypeCode,
            PersonName PersonName,
            List<Telephone> Telephone,
            List<Email> Email,
            List<TravelDocument> TravelDocument
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"@type", "Prefix", "Given", "Middle", "Surname"})
    record PersonName(
            @JsonProperty("@type") String type,
            String Prefix,
            String Given,
            String Middle,
            String Surname
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"@type", "countryAccessCode", "phoneNumber", "id", "cityCode", "role"})
    record Telephone(
            @JsonProperty("@type") String type,
            String countryAccessCode,
            String phoneNumber,
            String id,
            String cityCode,
            String role
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Email(String value) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"@type", "docNumber", "docType", "expireDate", "issueCountry", "birthDate", "Gender"})
    record TravelDocument(
            @JsonProperty("@type") String type,
            String docNumber,
            String docType,
            String expireDate,
            String issueCountry,
            String birthDate,
            String Gender
    ) {
    }

    /**
     * Agency/BSP form of payment - NOT the customer's card. Our own PaymentGateway has already
     * charged the customer and never retains a full card number, so the agency settles with
     * Travelport via its own account and the internal transaction reference is kept as a remark.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"@type", "id", "reservationFOPInd", "activeInd"})
    record FormOfPayment(
            @JsonProperty("@type") String type,
            String id,
            Boolean reservationFOPInd,
            Boolean activeInd
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonPropertyOrder({"@type", "Amount", "remark"})
    record Payment(
            @JsonProperty("@type") String type,
            Amount Amount,
            String remark
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Amount(double value, String code) {
    }
}
