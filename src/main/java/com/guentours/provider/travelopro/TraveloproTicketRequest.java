package com.guentours.provider.travelopro;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request body for Travelopro's {@code /api/aeroVE5/ticket} endpoint - issues the
 * e-ticket for a booking that was already created via {@code /book}. Best-effort
 * inference (not validated against a live sandbox response); confirm against
 * Travelopro's Ticket API docs before going live.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TraveloproTicketRequest(
        String user_id,
        String user_password,
        String access,
        String ip_address,
        String PNR
) {
}
