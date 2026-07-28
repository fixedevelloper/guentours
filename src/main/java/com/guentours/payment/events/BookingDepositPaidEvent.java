package com.guentours.payment.events;

/**
 * Publié quand un acompte (deposit) est payé — la réservation n'est pas encore confirmée.
 */
public record BookingDepositPaidEvent(
        String bookingId,
        String paymentId,
        String gatewayReference
) {}