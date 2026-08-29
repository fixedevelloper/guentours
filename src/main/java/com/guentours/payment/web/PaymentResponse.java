package com.guentours.payment.web;

import com.guentours.payment.domain.Payment;
import com.guentours.payment.domain.PaymentAuthorizationType;
import com.guentours.payment.domain.PaymentMethod;
import com.guentours.payment.domain.PaymentStatus;
import com.guentours.shared.Money;

public record PaymentResponse(
        String paymentId,
        String bookingId,
        Money amount,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        String failureReason,
        /** Set only while {@code status == PENDING_AUTHORIZATION} - which challenge the payer must complete. */
        PaymentAuthorizationType authorizationType,
        /** Set only when {@code authorizationType == REDIRECT} - where to send the payer (3DS). */
        String authorizationRedirectUrl,
        /** Set only when {@code authorizationType == CLIENT_ACTION} - the Stripe PaymentIntent client
         *  secret the frontend needs to call {@code stripe.confirmPayment} itself. */
        String authorizationClientSecret
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getBookingId(), payment.getAmount(),
                payment.getPaymentMethod(), payment.getStatus(), payment.getFailureReason(),
                payment.getAuthorizationType(), payment.getAuthorizationRedirectUrl(),
                payment.getAuthorizationClientSecret());
    }
}
