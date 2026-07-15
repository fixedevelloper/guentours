package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Add Payment request ({@code POST /air/paymentoffer/reservationworkbench/{sessionId}/payments}),
 * matching a verified real sample. Applies a form of payment (already added to the workbench) to
 * the offer(s); at commit, tickets/EMDs are issued for any offer payment was sent for. The body is
 * a top-level {@code @type: Payment} carrying the {@code Amount}, the {@code FormOfPaymentIdentifier}
 * referencing the FOP, and optionally the {@code OfferIdentifier}(s) being paid.
 *
 * <p>{@code OfferIdentifier} is omitted: with no offer specified, the payment applies to every
 * offer in the workbench, which is exactly what we want for our single-offer reservations. A
 * multi-offer booking (several air offers combined) would instead need explicit
 * {@code OfferIdentifier}(s) referencing the reservation's offer ids read from the post-commit
 * workbench.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TravelportPaymentRequest(
        @JsonProperty("@type") String type,
        Amount Amount,
        FormOfPaymentIdentifier FormOfPaymentIdentifier
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Amount(double value, String code) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record FormOfPaymentIdentifier(
            @JsonProperty("@type") String type,
            String id,
            String FormOfPaymentRef
    ) {
    }
}
