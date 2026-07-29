package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Add Payment request ({@code POST /air/paymentoffer/reservationworkbench/{sessionId}/payments}),
 * matching a verified real sample. Applies a form of payment (already added to the workbench) to
 * the offer(s); at commit, tickets/EMDs are issued for any offer payment was sent for.
 *
 * <p>A production reference client for this exact endpoint confirmed {@code Amount} carries
 * {@code minorUnit}/{@code currencySource} alongside {@code value}/{@code code}, and that
 * {@code FormOfPaymentIdentifier} has no {@code @type} discriminator (just {@code id}/
 * {@code FormOfPaymentRef}, referencing the id self-assigned in
 * {@link TravelportFormOfPaymentRequest}) - both corrections to an earlier, unverified guess.
 *
 * <p>{@code OfferIdentifier} is omitted: with no offer specified, the payment applies to every
 * offer in the workbench, which is exactly what we want for our single-offer reservations, and
 * {@code issueFlightTicket} only receives the PNR/payment - not the offer/catalog identifiers the
 * reference populates it with - so there's nothing to populate it from without a wider interface
 * change. A multi-offer booking would instead need explicit {@code OfferIdentifier}(s).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TravelportPaymentRequest(
        @JsonProperty("@type") String type,
        String id,
        Amount Amount,
        FormOfPaymentIdentifier FormOfPaymentIdentifier
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Amount(String code, Integer minorUnit, String currencySource, double value) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record FormOfPaymentIdentifier(
            String id,
            String FormOfPaymentRef
    ) {
    }
}
