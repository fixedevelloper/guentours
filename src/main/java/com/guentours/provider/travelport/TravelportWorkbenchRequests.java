package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    /** New Workbench body ({@code @type: Reservation}) - the container committed into a PNR. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Reservation(
            @JsonProperty("@type") String type,
            List<Offer> Offer,
            List<Traveler> Traveler,
            List<FormOfPayment> FormOfPayment,
            List<Payment> Payment
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Traveler(
            @JsonProperty("@type") String type,
            String birthDate,
            String passengerTypeCode,
            PersonName PersonName,
            List<Email> Email
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record PersonName(
            @JsonProperty("@type") String type,
            String Prefix,
            String Given,
            String Middle,
            String Surname
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Email(String value) {
    }

    /**
     * Agency/BSP form of payment - NOT the customer's card. Our own PaymentGateway has already
     * charged the customer and never retains a full card number, so the agency settles with
     * Travelport via its own account and the internal transaction reference is kept as a remark.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record FormOfPayment(
            @JsonProperty("@type") String type,
            String id,
            Boolean reservationFOPInd,
            Boolean activeInd
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
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
