package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Add Form of Payment request
 * ({@code POST /air/payment/reservationworkbench/{sessionId}/formofpayment?authorizePaymentInd=true}),
 * matching a verified real sample. The body is a top-level polymorphic form-of-payment object; the
 * {@code @type} discriminator selects the FOP kind (e.g. {@code FormOfPaymentCash},
 * {@code FormOfPaymentPaymentCard}). Cash and credit are supported for all content; agent invoice
 * and non-standard credit card are GDS-only.
 *
 * <p>GuenTours has already charged the customer through its own PaymentGateway, so a cash FOP is
 * added here (the agency has collected the funds and settles with Travelport), with the internal
 * transaction reference carried in {@code FreeText} for reconciliation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TravelportFormOfPaymentRequest(
        @JsonProperty("@type") String type,
        Boolean reservationFOPInd,
        Boolean activeInd,
        String Comment,
        String FreeText
) {
}
