package com.guentours.booking.event;

/** Internal to the booking module: marks that a payment cleared and provider ticket issuance can start. */
public record BookingPaidEvent(String bookingId, String paymentTransactionReference, String cardLast4) {
}
