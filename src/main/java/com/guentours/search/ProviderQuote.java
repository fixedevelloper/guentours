package com.guentours.search;

import com.guentours.provider.ProviderType;
import com.guentours.shared.Money;

/** One provider's price for a harmonized offer; {@code offerId} resolves back to the exact offer at checkout. */
public record ProviderQuote(String offerId, ProviderType providerType, Money price) {
}
