package com.guentours.provider.travelterminus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** {@code data} of {@code GET /api/flights/order-details/{bookingRefId}}. Despite the field
 *  reference table in the docs calling {@code pnr}/{@code flightTicketNo} string arrays, the
 *  actual captured sample response returns them as a single, possibly {@code " | "}-delimited
 *  string (one segment per leg/carrier) - modeled as {@code String} here to match the real
 *  payload, split on {@code "|"} where a list is needed. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TravelTerminusOrderDetailsResponse(
        String status,
        String bookingRefId,
        String bookingStatus,
        String pnr,
        String flightTicketNo,
        String totalAmount,
        String currencyCode
) {
}
