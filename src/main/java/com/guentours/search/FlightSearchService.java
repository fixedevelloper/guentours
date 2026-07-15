package com.guentours.search;

import com.guentours.provider.FlightOffer;
import com.guentours.provider.FlightSearchCriteria;
import com.guentours.provider.JourneyType;
import com.guentours.provider.ProviderType;
import com.guentours.provider.TravelProviderClient;
import com.guentours.shared.CommissionPolicy;
import com.guentours.shared.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
public class FlightSearchService {

    private static final Logger log = LoggerFactory.getLogger(FlightSearchService.class);

    private final List<TravelProviderClient> providerClients;
    private final ExecutorService providerSearchExecutor;
    private final FlightHarmonizer harmonizer;
    private final OfferCache offerCache;
    private final CommissionPolicy commissionPolicy;

    public FlightSearchService(List<TravelProviderClient> providerClients, ExecutorService providerSearchExecutor,
                                FlightHarmonizer harmonizer, OfferCache offerCache, CommissionPolicy commissionPolicy) {
        this.providerClients = providerClients;
        this.providerSearchExecutor = providerSearchExecutor;
        this.harmonizer = harmonizer;
        this.offerCache = offerCache;
        this.commissionPolicy = commissionPolicy;
    }

    public List<HarmonizedFlightOffer> search(FlightSearchRequest request) {
        JourneyType journeyType = request.journeyType() == null ? JourneyType.ONE_WAY : request.journeyType();
        if (journeyType == JourneyType.MULTI_CITY) {
            throw new IllegalArgumentException("Use POST /api/search/flights/multi-city for MULTI_CITY searches");
        }
        if (journeyType == JourneyType.ROUND_TRIP && request.returnDate() == null) {
            throw new IllegalArgumentException("returnDate is required for a ROUND_TRIP search");
        }

        FlightSearchCriteria criteria = new FlightSearchCriteria(
                request.origin().toUpperCase(), request.destination().toUpperCase(),
                request.departureDate(), request.returnDate(),
                request.adults() == null ? 1 : request.adults(),
                request.children() == null ? 0 : request.children(),
                request.infants() == null ? 0 : request.infants(),
                journeyType,
                request.cabinClass() == null ? "ECONOMY" : request.cabinClass().toUpperCase(),
                request.currency() == null ? "EUR" : request.currency().toUpperCase());

        return dispatchToProviders(criteria);
    }

    /**
     * Searches each leg independently as a ONE_WAY itinerary, then groups the raw offers by
     * provider into combined itineraries: for every provider that quoted every leg, sums that
     * provider's cheapest offer per leg into one bookable total. The customer picks one
     * itinerary (one provider, every destination already included) instead of a separate
     * offer per leg from potentially different providers.
     */
    public List<MultiCityItinerary> searchMultiCity(MultiCityFlightSearchRequest request) {
        int adults = request.adults() == null ? 1 : request.adults();
        int children = request.children() == null ? 0 : request.children();
        int infants = request.infants() == null ? 0 : request.infants();
        String cabinClass = request.cabinClass() == null ? "ECONOMY" : request.cabinClass().toUpperCase();
        String currency = request.currency() == null ? "EUR" : request.currency().toUpperCase();

        List<FlightLeg> legs = request.legs();
        List<Map<ProviderType, FlightOffer>> cheapestPerProviderByLeg = new ArrayList<>();
        for (FlightLeg leg : legs) {
            FlightSearchCriteria criteria = new FlightSearchCriteria(
                    leg.origin().toUpperCase(), leg.destination().toUpperCase(), leg.departureDate(), null,
                    adults, children, infants, JourneyType.ONE_WAY, cabinClass, currency);

            Map<ProviderType, FlightOffer> cheapestByProvider = new LinkedHashMap<>();
            for (FlightOffer offer : fetchRawOffers(criteria)) {
                cheapestByProvider.merge(offer.providerType(), offer,
                        (a, b) -> a.price().isLessThan(b.price()) ? a : b);
            }
            cheapestPerProviderByLeg.add(cheapestByProvider);
        }

        Set<ProviderType> commonProviders = new LinkedHashSet<>(cheapestPerProviderByLeg.get(0).keySet());
        for (Map<ProviderType, FlightOffer> byProvider : cheapestPerProviderByLeg) {
            commonProviders.retainAll(byProvider.keySet());
        }

        List<MultiCityItinerary> itineraries = new ArrayList<>();
        for (ProviderType provider : commonProviders) {
            List<MultiCityItineraryLeg> itineraryLegs = new ArrayList<>();
            Money total = null;
            for (int i = 0; i < legs.size(); i++) {
                FlightOffer offer = cheapestPerProviderByLeg.get(i).get(provider);
                String offerId = offerCache.cacheFlightOffer(offer);
                itineraryLegs.add(new MultiCityItineraryLeg(i, offer.airline(), offer.flightNumber(), offer.origin(),
                        offer.destination(), offer.departureTime(), offer.arrivalTime(), offer.cabinClass(), offerId));
                Money legPriceWithFee = commissionPolicy.addFlightFee(offer.price());
                total = total == null ? legPriceWithFee : total.add(legPriceWithFee);
            }
            itineraries.add(new MultiCityItinerary(provider, total, itineraryLegs));
        }
        itineraries.sort((a, b) -> a.totalPrice().compareTo(b.totalPrice()));
        return itineraries;
    }

    private List<FlightOffer> fetchRawOffers(FlightSearchCriteria criteria) {
        List<CompletableFuture<List<FlightOffer>>> futures = providerClients.stream()
                .filter(TravelProviderClient::isEnabled)
                .map(client -> CompletableFuture.supplyAsync(() -> {
                    log.info("Dispatching flight search to provider {}", client.getType());
                    return client.searchFlights(criteria);
                }, providerSearchExecutor))
                .toList();

        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();
    }

    private List<HarmonizedFlightOffer> dispatchToProviders(FlightSearchCriteria criteria) {
        return harmonizer.harmonize(fetchRawOffers(criteria));
    }
}
