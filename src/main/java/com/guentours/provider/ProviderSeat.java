package com.guentours.provider;

import com.guentours.shared.Money;

/**
 * One seat in a provider's seat map. {@code price} is null for a free seat (or when the provider
 * does not price seats); a non-null price marks a paid seat.
 */
public record ProviderSeat(String seatNumber, boolean available, Money price) {
}
