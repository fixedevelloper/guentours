package com.guentours.payment.gateway;

/** Abstraction over the real payment processor (Stripe, Adyen, ...) so it can be swapped without touching PaymentService. */
public interface PaymentGateway {

    ChargeResult charge(ChargeRequest request);
}
