package com.guentours.booking;

/**
 * Published once the customer has paid the non-refundable reservation fee of a PAY_LATER booking.
 * Carries only the booking id - listeners (e.g. the commission module, which records the fee as
 * reservation commission) fetch the rest via {@link BookingService#getById(String)}.
 */
public record ReservationFeePaidEvent(String bookingId) {
}
