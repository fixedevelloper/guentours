package com.guentours.booking;

/**
 * Published once a provider has confirmed the reservation and issued e-tickets.
 * Deliberately carries only the booking id - listeners fetch the rest via
 * {@link BookingService#getById(String)} so the (durably persisted) event
 * publication payload stays small and never goes stale.
 */
public record BookingConfirmedEvent(String bookingId) {
}
