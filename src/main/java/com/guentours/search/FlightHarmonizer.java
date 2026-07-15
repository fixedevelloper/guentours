package com.guentours.search;

import com.guentours.provider.FlightOffer;
import com.guentours.shared.CommissionPolicy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Groups raw offers collected from every provider by physical flight and keeps
 * every provider's price so the cheapest one can be highlighted, instead of
 * showing the client the same flight three times over.
 */
@Component
class FlightHarmonizer {

    private final OfferCache offerCache;
    private final CommissionPolicy commissionPolicy;

    FlightHarmonizer(OfferCache offerCache, CommissionPolicy commissionPolicy) {
        this.offerCache = offerCache;
        this.commissionPolicy = commissionPolicy;
    }

    List<HarmonizedFlightOffer> harmonize(List<FlightOffer> rawOffers) {
        Map<String, List<FlightOffer>> grouped = new LinkedHashMap<>();
        for (FlightOffer offer : rawOffers) {
            grouped.computeIfAbsent(offer.harmonizationKey(), k -> new ArrayList<>()).add(offer);
        }

        List<HarmonizedFlightOffer> result = new ArrayList<>();
        for (List<FlightOffer> group : grouped.values()) {
            List<ProviderQuote> quotes = new ArrayList<>();
            FlightOffer cheapest = group.get(0);
            for (FlightOffer offer : group) {
                String offerId = offerCache.cacheFlightOffer(offer);
                quotes.add(new ProviderQuote(offerId, offer.providerType(), commissionPolicy.addFlightFee(offer.price())));
                if (offer.price().isLessThan(cheapest.price())) {
                    cheapest = offer;
                }
            }
            quotes.sort((a, b) -> a.price().compareTo(b.price()));
            String cheapestOfferId = quotes.get(0).offerId();

            result.add(new HarmonizedFlightOffer(
                    cheapest.airline(), cheapest.flightNumber(), cheapest.origin(), cheapest.destination(),
                    cheapest.departureTime(), cheapest.arrivalTime(), cheapest.cabinClass(),
                    group.stream().mapToInt(FlightOffer::seatsAvailable).max().orElse(0),
                    cheapestOfferId, quotes));
        }
        result.sort((a, b) -> a.quotes().get(0).price().compareTo(b.quotes().get(0).price()));
        return result;
    }
}
