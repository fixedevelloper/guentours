package com.guentours.provider.sabre;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request body for Sabre's DCCI Payment v2 endpoint ({@code POST /v2/dcci/pay}) - the exact
 * path taken from the anchor of the doc URL supplied for this API ({@code #v2/dcci/pay_post}),
 * even though the page itself was not reachable to confirm the request/response schema.
 *
 * <p>Rather than resubmitting the traveler's card to Sabre, this submits an AGENCY-level form
 * of payment: our own {@code PaymentGateway} has already charged the customer directly and
 * never retains a full card number (only a last-4), so there is nothing PCI-sensitive left to
 * hand to a third-party GDS. The agency settles with Sabre via its own BSP/ARC account instead,
 * with our internal transaction reference kept only as a reconciliation remark. Field names
 * (in particular the form-of-payment type/account naming) are a best-effort guess pending
 * access to the real v2 spec.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record SabreSubmitPaymentRequest(
        String recordLocator,
        FormOfPayment formOfPayment,
        String remark
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record FormOfPayment(String type, String agencyAccountCode) {
    }
}
