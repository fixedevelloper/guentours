package com.guentours.booking;

import com.guentours.shared.Money;

/** Read-only projection handed to other modules (e.g. payment) that need booking facts without touching the entity. */
public record BookingSummary(
        String id,
        String userId,
        String contactEmail,
        BookingStatus status,
        Money price,
        PaymentPlan paymentPlan,
        Money depositAmount,
        Money amountDue
) {
    public static BookingSummary from(Booking booking) {
        return new BookingSummary(booking.getId(), booking.getUserId(), booking.getContactEmail(),
                booking.getStatus(), booking.getPrice(), booking.getPaymentPlan(), booking.getDepositAmount(),
                booking.amountDue());
    }
}
