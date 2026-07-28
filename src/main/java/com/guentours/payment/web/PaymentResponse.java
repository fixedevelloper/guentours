package com.guentours.payment.web;

import com.guentours.payment.domain.Payment;
import com.guentours.payment.domain.PaymentMethod;
import com.guentours.payment.domain.PaymentStatus;
import com.guentours.shared.Money;

public record PaymentResponse(
        String paymentId,
        String bookingId,
        Money amount,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        String failureReason
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getBookingId(), payment.getAmount(),
                payment.getPaymentMethod(), payment.getStatus(), payment.getFailureReason());
    }
}
