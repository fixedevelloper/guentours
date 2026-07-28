package com.guentours.search.domain;

import com.guentours.provider.VehicleOffer;
import com.guentours.search.OfferCache;
import com.guentours.shared.CommissionPolicy;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Groups raw vehicle offers collected from every provider by physical vehicle product.
 */
@Component
public class VehicleHarmonizer {

    private final OfferCache offerCache;
    private final CommissionPolicy commissionPolicy;

    VehicleHarmonizer(OfferCache offerCache, CommissionPolicy commissionPolicy) {
        this.offerCache = Objects.requireNonNull(offerCache, "offerCache must not be null");
        this.commissionPolicy = Objects.requireNonNull(commissionPolicy, "commissionPolicy must not be null");
    }

    public List<HarmonizedVehicleOffer> harmonize(List<VehicleOffer> rawOffers) {
        if (rawOffers == null || rawOffers.isEmpty()) {
            return List.of();
        }

        // Groupement par clé d'harmonisation en conservant l'ordre d'insertion
        Map<String, List<VehicleOffer>> grouped = rawOffers.stream()
                .collect(Collectors.groupingBy(
                        VehicleOffer::harmonizationKey,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return grouped.values().stream()
                .map(this::toHarmonizedOffer)
                .sorted(Comparator.comparing(h -> h.quotes().get(0).price()))
                .toList();
    }

    private HarmonizedVehicleOffer toHarmonizedOffer(List<VehicleOffer> group) {
        List<ProviderQuote> quotes = group.stream()
                .map(offer -> {
                    String offerId = offerCache.cacheVehicleOffer(offer);
                    var priceWithFee = commissionPolicy.addVehicleFee(offer.totalPrice());
                    return new ProviderQuote(offerId, offer.providerType(), priceWithFee);
                })
                .sorted(Comparator.comparing(ProviderQuote::price))
                .toList();

        VehicleOffer reference = group.get(0);
        return new HarmonizedVehicleOffer(
                reference.brand(),
                reference.model(),
                reference.category(),
                reference.transmission(),
                reference.seats(),
                reference.airConditioning(),
                reference.pickupCity(),
                reference.dropoffCity(),
                reference.rentalStart(),
                reference.pickupTime(),
                reference.rentalEnd(),
                reference.dropoffTime(),
                reference.withDriver(),
                reference.driverAge25Plus(),
                quotes.get(0).offerId(),
                quotes
        );
    }
}