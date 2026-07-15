package com.guentours.provider;

import java.util.List;

/**
 * A seat map returned by a provider for a specific flight offer: the cabin grid dimensions
 * ({@code rows} × {@code columns}) plus the per-seat availability. Returned by
 * {@link TravelProviderClient#seatMap(FlightOffer)} when the provider exposes real seat data;
 * callers fall back to a generic simulated map otherwise.
 */
public record ProviderSeatMap(int rows, List<String> columns, List<ProviderSeat> seats) {
}
