package com.guentours.search;

import com.guentours.provider.ProviderType;
import com.guentours.shared.Money;

import java.util.List;

/**
 * One provider's combined price for flying every leg of a MULTI_CITY search - the unit the
 * customer actually selects and books, rather than picking a separate offer per leg.
 */
public record MultiCityItinerary(
        ProviderType providerType,
        Money totalPrice,
        List<MultiCityItineraryLeg> legs
) {
}
