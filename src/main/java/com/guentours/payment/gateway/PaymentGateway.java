package com.guentours.payment.gateway;

/** Abstraction over the real payment processor (Stripe, Adyen, ...) so it can be swapped without touching PaymentService. */
public interface PaymentGateway {

    ChargeResult charge(ChargeRequest request);

    /**
     * Completes a card charge that came back {@code PENDING_AUTHORIZATION} with a PIN, once the payer
     * has entered it. {@code originalRequest} is the exact request the initial {@link #charge} call
     * used, so the gateway can resend the card details the challenge requires alongside the PIN.
     * Gateways that never return a PIN challenge from {@link #charge} don't need to override this.
     */
    default ChargeResult completeCardPinAuthorization(String paymentId, ChargeRequest originalRequest, String pin) {
        throw new UnsupportedOperationException("This gateway does not support card PIN authorization");
    }
}
