package com.guentours.payment.domain;

/**
 * Mirrors {@code com.guentours.payment.gateway.AuthorizationChallenge.AuthorizationType} without the
 * domain depending on the gateway package - {@link com.guentours.payment.service.PaymentService}
 * translates between the two at the boundary.
 */
public enum PaymentAuthorizationType {
    PIN,
    AVS,
    REDIRECT,
    OTP
}
