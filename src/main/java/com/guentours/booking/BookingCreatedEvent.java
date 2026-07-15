package com.guentours.booking;

/**
 * Published once a booking (single offer or MULTI_CITY) has been persisted at checkout time.
 * Carries only the booking id - listeners (e.g. the commission module) fetch the rest via
 * {@link BookingService#getById(String)} so the payload stays small and never goes stale.
 */
public record BookingCreatedEvent(String bookingId) {
}
