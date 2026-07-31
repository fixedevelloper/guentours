package com.guentours.payment.domain;

public enum PaymentStatus {
    PENDING,
    /** Awaiting a synchronous authorization step (card PIN/AVS/3DS redirect) before it can settle. */
    PENDING_AUTHORIZATION,
    SUCCEEDED,
    FAILED
}
