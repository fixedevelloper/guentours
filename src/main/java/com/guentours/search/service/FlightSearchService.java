package com.guentours.search.service;

import com.guentours.provider.FlightOffer;
import com.guentours.provider.FlightSearchCriteria;
import com.guentours.provider.JourneyType;
import com.guentours.provider.ProviderType;
import com.guentours.provider.TravelProviderClient;
import com.guentours.search.*;
import com.guentours.search.domain.*;
import com.guentours.search.web.FlightSearchRequest;
import com.guentours.search.web.MultiCityFlightSearchRequest;
import com.guentours.shared.CommissionPolicy;
import com.guentours.shared.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class FlightSearchService {

    private static final Logger log = LoggerFactory.getLogger(FlightSearchService.class);

    private final List<TravelProviderClient> providerClients;
    private final ExecutorService providerSearchExecutor;
    private final FlightHarmonizer harmonizer;
    private final OfferCache offerCache;
    private final CommissionPolicy commissionPolicy;
    /**
     * Hard ceiling on how long any single provider gets to answer a fan-out search, so the page
     * can guarantee results within its own budget (currently 15s end-to-end) no matter how many
     * providers are enabled or how slow one of them is. A provider that misses this window is
     * treated as having returned no offers rather than blocking the whole response - the caller
     * still gets every other provider's results on time. This is independent of (and normally
     * shorter than) each vendor client's own connect/read timeout, which exists to bound the
     * underlying HTTP call itself.
     */
    private final long providerTimeoutMillis;

    public FlightSearchService(List<TravelProviderClient> providerClients, ExecutorService providerSearchExecutor,
                               FlightHarmonizer harmonizer, OfferCache offerCache, CommissionPolicy commissionPolicy,
                               @Value("${app.search.flight-provider-timeout-millis:12000}") long providerTimeoutMillis) {
        this.providerClients = providerClients;
        this.providerSearchExecutor = providerSearchExecutor;
        this.harmonizer = harmonizer;
        this.offerCache = offerCache;
        this.commissionPolicy = commissionPolicy;
        this.providerTimeoutMillis = providerTimeoutMillis;
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
                        (a, b) -> PriceOrdering.isCheaper(a.price(), b.price()) ? a : b);
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
                itineraryLegs.add(new MultiCityItineraryLeg(i, offer.airline(), offer.airlineName(), offer.flightNumber(),
                        offer.origin(), offer.destination(), offer.departureTime(), offer.arrivalTime(),
                        offer.cabinClass(), offerId, offer.detail()));
                Money legPriceWithFee = commissionPolicy.addFlightFee(offer.price());
                total = total == null ? legPriceWithFee : total.add(legPriceWithFee);
            }
            itineraries.add(new MultiCityItinerary(provider, total, itineraryLegs));
        }
        itineraries.sort(Comparator.comparing(MultiCityItinerary::totalPrice, PriceOrdering.CHEAPEST_FIRST));
        return itineraries;
    }

    private List<FlightOffer> fetchRawOffers(FlightSearchCriteria criteria) {
        List<CompletableFuture<List<FlightOffer>>> futures = providerClients.stream()
                .filter(TravelProviderClient::isEnabled)
                .map(client -> CompletableFuture.supplyAsync(() -> {
                            log.info("Dispatching flight search to provider {}", client.getType());
                            return client.searchFlights(criteria);
                        }, providerSearchExecutor)
                        .orTimeout(providerTimeoutMillis, TimeUnit.MILLISECONDS)
                        .exceptionally(ex -> {
                            log.warn("Provider {} did not respond within {} ms, excluding it from these results: {}",
                                    client.getType(), providerTimeoutMillis, ex.toString());
                            return List.of();
                        }))
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
