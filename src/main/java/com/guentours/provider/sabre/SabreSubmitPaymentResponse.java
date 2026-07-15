package com.guentours.provider.sabre;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Response body of Sabre's DCCI Payment v2 endpoint ({@code POST /v2/dcci/pay}). Sabre's
 * NDC/DCCI order model generally auto-issues e-tickets once a form of payment clears against
 * the order, rather than requiring a separate ticketing call - so {@code ticketed}/{@code
 * eTicketNumbers} double as the final ticketing confirmation. Verify field names against the
 * real v2 spec once reachable.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record SabreSubmitPaymentResponse(
        boolean ticketed,
        List<String> eTicketNumbers,
        String status
) {
}
