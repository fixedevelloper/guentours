package com.guentours.ticketing;

import jakarta.validation.constraints.NotBlank;

/**
 * {@code requesterEmail} proves the caller owns this booking (same guest-access rule as every
 * other {@code /api/tickets/**}/{@code /api/bookings/**} read). {@code recipientEmail} is optional
 * - when blank, the ticket is sent to the requester's own address.
 */
public record TicketEmailRequest(@NotBlank String requesterEmail, String recipientEmail) {
}
