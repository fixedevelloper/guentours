package com.guentours.booking;

public enum PaymentPlan {
    /** Full price charged immediately at checkout. */
    PAY_NOW,
    /** Only a reservation deposit is charged now; the balance is due before {@code ticketingDeadline}. */
    PAY_LATER
}
