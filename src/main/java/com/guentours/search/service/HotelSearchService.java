package com.guentours.search.service;

import com.guentours.provider.*;
import com.guentours.search.domain.HarmonizedHotelOffer;
import com.guentours.search.domain.HotelHarmonizer;
import com.guentours.search.web.HotelSearchRequest;
import com.guentours.search.OfferCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HotelSearchService {

    private static final Logger log = LoggerFactory.getLogger(HotelSearchService.class);

    private final Map<ProviderType, TravelProviderClient> providerClientsMap;
    private final List<TravelProviderClient> providerClients;
    private final ExecutorService providerSearchExecutor;
    private final HotelHarmonizer harmonizer;
    private final OfferCache offerCache;

    public HotelSearchService(List<TravelProviderClient> providerClients,
                              ExecutorService providerSearchExecutor,
                              HotelHarmonizer harmonizer,
                              OfferCache offerCache) {
        this.providerClients = providerClients;
        // Indexation sécurisée des clients par leur type (ProviderType)
        this.providerClientsMap = providerClients.stream()
                .collect(Collectors.toMap(TravelProviderClient::getType, Function.identity()));
        this.providerSearchExecutor = providerSearchExecutor;
        this.harmonizer = harmonizer;
        this.offerCache = offerCache;
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

    public List<HarmonizedHotelOffer> search(HotelSearchRequest request) {
        HotelSearchCriteria criteria = new HotelSearchCriteria(
                request.cityCode().toUpperCase(),
                request.checkIn(),
                request.checkOut(),
                request.adults() == null ? 1 : request.adults(),
                request.rooms() == null ? 1 : request.rooms(),
                request.currency() == null ? "XAF" : request.currency()
        );

        List<CompletableFuture<List<HotelOffer>>> futures = providerClients.stream()
                .filter(TravelProviderClient::isEnabled)
                .map(client -> CompletableFuture.supplyAsync(() -> {
                            log.info("Dispatching hotel search to provider {}", client.getType());
                            try {
                                List<HotelOffer> offers = client.searchHotels(criteria);
                                log.info("Provider {} returned {} offers", client.getType(), offers != null ? offers.size() : 0);
                                return offers != null ? offers : List.<HotelOffer>of();
                            } catch (Exception e) {
                                log.error("Error executing hotel search on provider {}: {}", client.getType(), e.getMessage(), e);
                                return List.<HotelOffer>of();
                            }
                        }, providerSearchExecutor)
                        // Timeout de sécurité (5 secondes max par provider pour éviter de bloquer indéfiniment)
                        .orTimeout(5, TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            log.error("Provider timed out or failed unexpectedly: {}", ex.getMessage());
                            return List.of();
                        }))
                .toList();

        // Agrégation et harmonisation des résultats
        List<HotelOffer> allOffers = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();

        return harmonizer.harmonize(allOffers);
    }

    private TravelProviderClient clientFor(ProviderType providerType) {
        TravelProviderClient client = providerClientsMap.get(providerType);
        if (client == null) {
            throw new IllegalStateException("Aucun adaptateur enregistré pour le fournisseur : " + providerType);
        }
        return client;
    }
}