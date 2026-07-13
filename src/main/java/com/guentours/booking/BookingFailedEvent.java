package com.guentours.booking;

/** Published when the provider rejects (or times out on) confirming an already-paid booking. */
public record BookingFailedEvent(String bookingId) {
}
