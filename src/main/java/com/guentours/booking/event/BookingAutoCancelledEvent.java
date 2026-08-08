package com.guentours.booking.event;

/** Fired when a booking is cancelled automatically by a scheduler rather than by the user. */
public record BookingAutoCancelledEvent(String bookingId, String reason) {
}
