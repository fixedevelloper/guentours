package com.guentours.payment.events;

/** Fired whenever a payment attempt is marked failed, whatever the cause (gateway error, declined
 *  charge, unsupported verification method, expired authorization session). */
public record PaymentFailedEvent(String bookingId, String paymentId, String reason) {
}
