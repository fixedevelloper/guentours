package com.guentours.payment.events;

/**
 * Publié quand le paiement complet est confirmé et la réservation passe à l'état confirmé.
 * C'est cet événement qui doit déclencher la commission revendeur.
 */
public record BookingFullyPaidEvent(
        String bookingId,
        String paymentId,
        String gatewayReference,
        String payerReferenceLast4
) {}