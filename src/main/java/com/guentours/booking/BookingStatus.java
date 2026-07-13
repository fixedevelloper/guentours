package com.guentours.booking;

public enum BookingStatus {
    PENDING_PAYMENT,
    /** Reservation deposit paid under a PAY_LATER plan; balance still due before ticketingDeadline. */
    DEPOSIT_PAID,
    PAID,
    CONFIRMING,
    CONFIRMED,
    FAILED,
    CANCELLED
}
