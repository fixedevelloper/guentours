package com.guentours.provider.travelterminus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** {@code data} of the Book response. Note this is returned with HTTP 200 even for a business
 *  failure (no availability / session timeout) - {@code error}/{@code status} must be checked
 *  explicitly, an HTTP-level try/catch is not enough. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TravelTerminusBookResponse(
        String status,
        Boolean error,
        String searchReqId,
        String currency,
        Double orderAmount,
        Double walletPoints,
        String bookingRefId,
        List<OrderStatusEntry> orderDetails
) {
    /** {@code orderStatus} - one of Confirmed/Pending/Failed/Cancelled/Completed/InProcess/
     *  CancelRequested/Void (the pre-2026-08-13 Hold value was retired, see the changelog). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record OrderStatusEntry(String orderStatus, String pnr, String message) {
    }
}
