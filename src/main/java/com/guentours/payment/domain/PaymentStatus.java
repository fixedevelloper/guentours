package com.guentours.payment.domain;

public enum PaymentStatus {
    PENDING,
    /** Awaiting a synchronous authorization step (card PIN/AVS/3DS redirect) before it can settle. */
    PENDING_AUTHORIZATION,
    SUCCEEDED,
    FAILED,
    /** Reached only from SUCCEEDED via an admin-initiated refund (see PaymentService#refundForBooking) -
     *  typically after the provider declined final confirmation on an already-captured payment. */
    REFUNDED
}
