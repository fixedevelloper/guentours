package com.guentours.booking.domain;

public enum BookingStatus {
    /** Booking row created, provider hold still in flight in the background - no PNR yet. */
    PENDING_HOLD,
    PENDING_PAYMENT,
    /** Reservation deposit paid under a PAY_LATER plan; balance still due before ticketingDeadline. */
    DEPOSIT_PAID,
    PAID,
    CONFIRMING,
    CONFIRMED,
    FAILED,
    CANCELLED
}
