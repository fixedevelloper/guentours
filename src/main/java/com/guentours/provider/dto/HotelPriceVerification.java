package com.guentours.provider.dto;

import com.guentours.shared.Money;

/**
 * Fresh price/availability re-check for a hotel offer, fetched directly from the
 * provider right before a booking hold is created. {@code verifiedPrice} is null when
 * the provider has nothing fresher than what was already quoted at search time - callers
 * should then trust the originally quoted price rather than treat it as a price of zero.
 */
public record HotelPriceVerification(
        String hotelOfferId,
        Money verifiedPrice,
        boolean available,
        String cancellationPolicy
) {
    /** True when the provider returned a fresh price that differs from what was quoted at search time. */
    public boolean priceChanged(Money quotedPrice) {
        return quotedPrice != null && verifiedPrice != null && verifiedPrice.amount().compareTo(quotedPrice.amount()) != 0;
    }
}
