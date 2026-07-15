package com.guentours.commission;

/** What a commission wallet entry represents. */
public enum CommissionType {
    /** The fixed fee GuenTours adds on top of every booking's provider price, recorded at checkout. */
    BOOKING_FEE,
    /** The non-refundable reservation fee a PAY_LATER customer pays to hold the booking. */
    RESERVATION_FEE
}
