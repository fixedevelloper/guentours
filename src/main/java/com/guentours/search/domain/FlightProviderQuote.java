package com.guentours.search.domain;

import com.guentours.provider.FlightOfferDetail;
import com.guentours.provider.ProviderType;
import com.guentours.shared.Money;

/**
 * One provider's price for a harmonized flight, like {@link ProviderQuote} plus {@code detail}
 * (stops, baggage, hold availability). Kept as a flight-only type rather than added to the shared
 * {@link ProviderQuote} (also used by hotel/property/vehicle harmonizers, which have no such
 * concept) - {@code detail} is {@code null} for providers that don't surface it.
 */
public record FlightProviderQuote(String offerId, ProviderType providerType, Money price, FlightOfferDetail detail) {
}
