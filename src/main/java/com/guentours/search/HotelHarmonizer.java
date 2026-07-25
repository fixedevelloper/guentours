package com.guentours.search;

import com.guentours.provider.HotelOffer;
import com.guentours.shared.CommissionPolicy;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Groups raw hotel offers collected from every provider by physical room product.
 */
@Component
class HotelHarmonizer {

    private final OfferCache offerCache;
    private final CommissionPolicy commissionPolicy;

    HotelHarmonizer(OfferCache offerCache, CommissionPolicy commissionPolicy) {
        this.offerCache = Objects.requireNonNull(offerCache, "offerCache must not be null");
        this.commissionPolicy = Objects.requireNonNull(commissionPolicy, "commissionPolicy must not be null");
    }

    List<HarmonizedHotelOffer> harmonize(List<HotelOffer> rawOffers) {
        if (rawOffers == null || rawOffers.isEmpty()) {
            return List.of();
        }

        // Groupement par clé d'harmonisation en conservant l'ordre d'insertion
        Map<String, List<HotelOffer>> grouped = rawOffers.stream()
                .collect(Collectors.groupingBy(
                        HotelOffer::harmonizationKey,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return grouped.values().stream()
                .map(this::toHarmonizedOffer)
                .sorted(Comparator.comparing(h -> h.quotes().get(0).price()))
                .toList();
    }

    private HarmonizedHotelOffer toHarmonizedOffer(List<HotelOffer> group) {
        List<ProviderQuote> quotes = group.stream()
                .map(offer -> {
                    String offerId = offerCache.cacheHotelOffer(offer);
                    var priceWithFee = commissionPolicy.addHotelFee(offer.price());
                    return new ProviderQuote(offerId, offer.providerType(), priceWithFee);
                })
                .sorted(Comparator.comparing(ProviderQuote::price))
                .toList();

        HotelOffer reference = group.get(0);

        return new HarmonizedHotelOffer(
                reference.hotelName(),
                reference.cityCode(),
                reference.roomType(),
                reference.checkIn(),
                reference.checkOut(),
                reference.rating(),
                quotes.get(0).offerId(),
                quotes
        );
    }
}