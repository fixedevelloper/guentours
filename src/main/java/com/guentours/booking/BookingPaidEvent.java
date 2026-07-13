package com.guentours.booking;

/** Internal to the booking module: marks that a payment cleared and provider ticket issuance can start. */
record BookingPaidEvent(String bookingId, String paymentTransactionReference, String cardLast4) {
}
