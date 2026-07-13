package com.guentours.search;

import com.guentours.provider.HotelOffer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Groups raw hotel offers collected from every provider by physical room product. */
@Component
class HotelHarmonizer {

    private final OfferCache offerCache;

    HotelHarmonizer(OfferCache offerCache) {
        this.offerCache = offerCache;
    }

    List<HarmonizedHotelOffer> harmonize(List<HotelOffer> rawOffers) {
        Map<String, List<HotelOffer>> grouped = new LinkedHashMap<>();
        for (HotelOffer offer : rawOffers) {
            grouped.computeIfAbsent(offer.harmonizationKey(), k -> new ArrayList<>()).add(offer);
        }

        List<HarmonizedHotelOffer> result = new ArrayList<>();
        for (List<HotelOffer> group : grouped.values()) {
            List<ProviderQuote> quotes = new ArrayList<>();
            for (HotelOffer offer : group) {
                String offerId = offerCache.cacheHotelOffer(offer);
                quotes.add(new ProviderQuote(offerId, offer.providerType(), offer.price()));
            }
            quotes.sort((a, b) -> a.price().compareTo(b.price()));
            HotelOffer reference = group.get(0);

            result.add(new HarmonizedHotelOffer(
                    reference.hotelName(), reference.cityCode(), reference.roomType(),
                    reference.checkIn(), reference.checkOut(), reference.rating(),
                    quotes.get(0).offerId(), quotes));
        }
        result.sort((a, b) -> a.quotes().get(0).price().compareTo(b.quotes().get(0).price()));
        return result;
    }
}
