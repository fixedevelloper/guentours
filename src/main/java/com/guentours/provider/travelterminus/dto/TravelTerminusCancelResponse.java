package com.guentours.provider.travelterminus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** {@code data} of the Cancel Request response. {@code status} is always {@code "success"} for an
 *  accepted request - acceptance only means the cancellation was queued for manual review by a
 *  Travel Terminus operator, not that the booking is actually cancelled/refunded yet. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TravelTerminusCancelResponse(String status) {
}
