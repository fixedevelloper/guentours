package com.guentours.search.service;

import com.guentours.geo.HotelCity;
import com.guentours.geo.HotelCityRepository;
import com.guentours.provider.*;
import com.guentours.search.domain.HarmonizedHotelOffer;
import com.guentours.search.domain.HotelHarmonizer;
import com.guentours.search.domain.HotelSearchResult;
import com.guentours.search.web.HotelSearchRequest;
import com.guentours.search.OfferCache;
import com.guentours.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HotelSearchService {

    private static final Logger log = LoggerFactory.getLogger(HotelSearchService.class);
    private static final int PROVIDER_SEARCH_TIMEOUT_SECONDS = 30;

    private final Map<ProviderType, TravelProviderClient> providerClientsMap;
    private final List<TravelProviderClient> providerClients;
    private final ExecutorService providerSearchExecutor;
    private final HotelHarmonizer harmonizer;
    private final OfferCache offerCache;
    private final HotelCityRepository hotelCityRepository;

    public HotelSearchService(List<TravelProviderClient> providerClients,
                              ExecutorService providerSearchExecutor,
                              HotelHarmonizer harmonizer,
                              OfferCache offerCache,
                              HotelCityRepository hotelCityRepository) {
        this.providerClients = providerClients;
        // Indexation sécurisée des clients par leur type (ProviderType)
        this.providerClientsMap = providerClients.stream()
                .collect(Collectors.toMap(TravelProviderClient::getType, Function.identity()));
        this.providerSearchExecutor = providerSearchExecutor;
        this.harmonizer = harmonizer;
        this.offerCache = offerCache;
        this.hotelCityRepository = hotelCityRepository;
    }

    public HotelDetail getDetailHotel(String offerId) {
        // Recherche sécurisée en cache avec message d'erreur explicite
        HotelOffer offer = offerCache.getHotelOffer(offerId)
                .orElseThrow(() -> new NoSuchElementException("L'offre sélectionnée a expiré ou n'existe pas : " + offerId));

        TravelProviderClient client = clientFor(offer.providerType());
        return client.getDetailHotel(offer);
    }
    public List<RoomOffer> getRoomHotels(String offerId) {
        // Recherche sécurisée en cache avec message d'erreur explicite
        HotelOffer offer = offerCache.getHotelOffer(offerId)
                .orElseThrow(() -> new NoSuchElementException("L'offre sélectionnée a expiré ou n'existe pas : " + offerId));

        TravelProviderClient client = clientFor(offer.providerType());
        return client.getRoomOffers(offer);
    }

    public HotelSearchResult search(HotelSearchRequest request) {
        HotelSearchCriteria criteria = toCriteria(request);

        Map<ProviderType, List<HotelOffer>> offersByProvider = dispatchToProviders(
                client -> client.searchHotels(criteria));

        Map<ProviderType, String> searchIdentifiers = captureSearchIdentifiers(offersByProvider);
        List<HotelOffer> allOffers = offersByProvider.values().stream().flatMap(List::stream).toList();
        List<HarmonizedHotelOffer> harmonized = harmonizer.harmonize(allOffers);

        String searchId = searchIdentifiers.isEmpty() ? null
                : offerCache.cacheHotelSearchSession(criteria, searchIdentifiers);

        return new HotelSearchResult(searchId, harmonized);
    }

    /**
     * Resumes a search started via {@link #search} for another page of results, re-running only
     * the providers that actually captured a pagination token on page 1 (see
     * {@code OfferCache.HotelSearchSession}) - a provider without one simply has nothing more to
     * page through for this search.
     */
    public List<HarmonizedHotelOffer> loadMore(String searchId, int pageNumber) {
        OfferCache.HotelSearchSession session = offerCache.getHotelSearchSession(searchId)
                .orElseThrow(() -> new NotFoundException("Cette recherche a expiré, veuillez relancer une recherche"));

        Map<ProviderType, List<HotelOffer>> offersByProvider = dispatchToProviders(session.providerSearchIdentifiers(),
                (client, identifier) -> client.loadMoreHotels(session.criteria(), identifier, pageNumber));

        List<HotelOffer> allOffers = offersByProvider.values().stream().flatMap(List::stream).toList();
        return harmonizer.harmonize(allOffers);
    }

    private HotelSearchCriteria toCriteria(HotelSearchRequest request) {
        Optional<HotelCity> city = hotelCityRepository.search(request.cityCode(), PageRequest.of(0, 1))
                .stream().findFirst();
        return new HotelSearchCriteria(
                request.cityCode().toUpperCase(),
                request.checkIn(),
                request.checkOut(),
                request.adults() == null ? 1 : request.adults(),
                request.rooms() == null ? 1 : request.rooms(),
                request.currency() == null ? "XAF" : request.currency(),
                city.map(HotelCity::getLatitude).orElse(null),
                city.map(HotelCity::getLongitude).orElse(null)
        );
    }

    /** Fans a call out to every enabled provider in parallel, capped by the same safety timeout
     *  as an ordinary search - a provider that errors or times out just contributes no offers. */
    private Map<ProviderType, List<HotelOffer>> dispatchToProviders(Function<TravelProviderClient, List<HotelOffer>> call) {
        List<TravelProviderClient> enabledClients = providerClients.stream().filter(TravelProviderClient::isEnabled).toList();
        return runInParallel(enabledClients, call);
    }

    /** Same fan-out, but only for the providers that have a pagination token in {@code identifiers}. */
    private Map<ProviderType, List<HotelOffer>> dispatchToProviders(Map<ProviderType, String> identifiers,
                                                                     java.util.function.BiFunction<TravelProviderClient, String, List<HotelOffer>> call) {
        List<TravelProviderClient> clients = identifiers.keySet().stream()
                .map(providerClientsMap::get)
                .filter(client -> client != null && client.isEnabled())
                .toList();
        return runInParallel(clients, client -> call.apply(client, identifiers.get(client.getType())));
    }

    private Map<ProviderType, List<HotelOffer>> runInParallel(List<TravelProviderClient> clients,
                                                               Function<TravelProviderClient, List<HotelOffer>> call) {
        List<CompletableFuture<Map.Entry<ProviderType, List<HotelOffer>>>> futures = clients.stream()
                .map(client -> CompletableFuture.supplyAsync(() -> {
                            log.info("Dispatching hotel search to provider {}", client.getType());
                            try {
                                List<HotelOffer> offers = call.apply(client);
                                log.info("Provider {} returned {} offers", client.getType(), offers != null ? offers.size() : 0);
                                return Map.entry(client.getType(), offers != null ? offers : List.<HotelOffer>of());
                            } catch (Exception e) {
                                log.error("Error executing hotel search on provider {}: {}", client.getType(), e.getMessage(), e);
                                return Map.entry(client.getType(), List.<HotelOffer>of());
                            }
                        }, providerSearchExecutor)
                        // Timeout de sécurité pour éviter de bloquer indéfiniment - aligné sur le
                        // timeout HTTP par défaut d'un provider (30s, cf. ProviderProperties.Vendor),
                        // 5s coupait des réponses de provider encore légitimes en cours de traitement.
                        .orTimeout(PROVIDER_SEARCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            log.error("Provider timed out or failed unexpectedly: {}", ex.getMessage());
                            return Map.entry(client.getType(), List.<HotelOffer>of());
                        }))
                .toList();

        Map<ProviderType, List<HotelOffer>> result = new HashMap<>();
        for (CompletableFuture<Map.Entry<ProviderType, List<HotelOffer>>> future : futures) {
            Map.Entry<ProviderType, List<HotelOffer>> entry = future.join();
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /** A provider's search-level pagination token (see {@code HotelOffer#context},
     *  key "searchIdentifier") is the same on every offer it returned - read it off the first one. */
    private Map<ProviderType, String> captureSearchIdentifiers(Map<ProviderType, List<HotelOffer>> offersByProvider) {
        Map<ProviderType, String> identifiers = new HashMap<>();
        offersByProvider.forEach((providerType, offers) -> {
            if (offers.isEmpty()) {
                return;
            }
            String identifier = offers.get(0).context("searchIdentifier");
            if (identifier != null && !identifier.isBlank()) {
                identifiers.put(providerType, identifier);
            }
        });
        return identifiers;
    }

    private TravelProviderClient clientFor(ProviderType providerType) {
        TravelProviderClient client = providerClientsMap.get(providerType);
        if (client == null) {
            throw new IllegalStateException("Aucun adaptateur enregistré pour le fournisseur : " + providerType);
        }
        return client;
    }
}
