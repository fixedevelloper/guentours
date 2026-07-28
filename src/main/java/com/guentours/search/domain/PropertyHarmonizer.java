package com.guentours.search.domain;

import com.guentours.provider.PropertyOffer;
import com.guentours.search.OfferCache;
import com.guentours.shared.CommissionPolicy;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class PropertyHarmonizer {

    private final OfferCache offerCache;
    private final CommissionPolicy commissionPolicy;

    PropertyHarmonizer(OfferCache offerCache, CommissionPolicy commissionPolicy) {
        this.offerCache = Objects.requireNonNull(offerCache, "offerCache must not be null");
        this.commissionPolicy = Objects.requireNonNull(commissionPolicy, "commissionPolicy must not be null");
    }

    public List<HarmonizedPropertyOffer> harmonize(List<PropertyOffer> rawOffers) {
        if (rawOffers == null || rawOffers.isEmpty()) {
            return List.of();
        }

        Map<String, List<PropertyOffer>> grouped = rawOffers.stream()
                .collect(Collectors.groupingBy(
                        PropertyOffer::harmonizationKey,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return grouped.values().stream()
                .map(this::toHarmonizedOffer)
                .sorted(Comparator.comparing(h -> h.quotes().get(0).price()))
                .toList();
    }

    private HarmonizedPropertyOffer toHarmonizedOffer(List<PropertyOffer> group) {
        List<ProviderQuote> quotes = group.stream()
                .map(offer -> {
                    String offerId = offerCache.cachePropertyOffer(offer);
                    var priceWithFee = commissionPolicy.addPropertyFee(offer.totalPrice());
                    return new ProviderQuote(offerId, offer.providerType(), priceWithFee);
                })
                .sorted(Comparator.comparing(ProviderQuote::price))
                .toList();

        PropertyOffer reference = group.get(0);
        return new HarmonizedPropertyOffer(
                reference.title(),
                reference.propertyType(),
                reference.city(),
                reference.country(),
                reference.bedrooms(),
                reference.maxGuests(),
                reference.entirePlace(),
                reference.checkIn(),
                reference.checkOut(),
                quotes.get(0).offerId(),
                quotes
        );
    }
}