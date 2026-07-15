package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response envelope of Travelport's Add Form of Payment
 * ({@code POST /air/payment/reservationworkbench/{sessionId}/formofpayment}). Returns the created
 * {@code FormOfPayment} with its {@code id}/{@code FormOfPaymentRef}, which the subsequent Add
 * Payment step references. Verified against a real sample.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record TravelportFormOfPaymentResponse(FormOfPaymentResponse FormOfPaymentResponse) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FormOfPaymentResponse(
            FormOfPayment FormOfPayment,
            String reservationStatus,
            TravelportSearchResponse.Result Result
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FormOfPayment(
            @JsonProperty("@type") String type,
            String id,
            String FormOfPaymentRef
    ) {
    }
}
