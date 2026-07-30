package com.guentours.search.service;

import com.guentours.provider.*;
import com.guentours.search.*;
import com.guentours.search.domain.HarmonizedPropertyOffer;
import com.guentours.search.domain.PropertyHarmonizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class PropertySearchService {

    private static final Logger log = LoggerFactory.getLogger(PropertySearchService.class);
    private static final int PROVIDER_SEARCH_TIMEOUT_SECONDS = 30;

    private final Map<ProviderType, TravelProviderClient> providerClientsMap;
    private final List<TravelProviderClient> providerClients;
    private final ExecutorService providerSearchExecutor;
    private final PropertyHarmonizer harmonizer;
    private final OfferCache offerCache;

    public PropertySearchService(Map<ProviderType, TravelProviderClient> providerClientsMap,
                                 List<TravelProviderClient> providerClients, ExecutorService providerSearchExecutor,
                                 PropertyHarmonizer harmonizer, OfferCache offerCache) {
        this.providerClientsMap = providerClientsMap;
        this.providerClients = providerClients;
        this.providerSearchExecutor = providerSearchExecutor;
        this.harmonizer = harmonizer;
        this.offerCache = offerCache;
    }

    public List<HarmonizedPropertyOffer> search(PropertySearchCriteria request) {
        PropertySearchCriteria criteria = new PropertySearchCriteria(
                request.city().toUpperCase(),
                request.checkIn(),
                request.checkOut(),
                request.guests(),
                request.bedrooms(),
                request.propertyType(),
                request.entirePlace(),
                request.currency() == null ? "XAF" : request.currency()
        );

        List<CompletableFuture<List<PropertyOffer>>> futures = providerClients.stream()
                .filter(TravelProviderClient::isEnabled)
                .map(client -> CompletableFuture.supplyAsync(() -> {
                            log.info("Dispatching property search to provider {}", client.getType());
                            try {
                                List<PropertyOffer> offers = client.searchProperties(criteria);
                                log.info("Provider {} returned {} offers", client.getType(), offers != null ? offers.size() : 0);
                                return offers != null ? offers : List.<PropertyOffer>of();
                            } catch (Exception e) {
                                log.error("Error executing property search on provider {}: {}", client.getType(), e.getMessage(), e);
                                return List.<PropertyOffer>of();
                            }
                        }, providerSearchExecutor)
                        // Aligné sur le timeout HTTP par défaut d'un provider (30s, cf.
                        // ProviderProperties.Vendor) - 5s coupait des réponses encore légitimes.
                        .orTimeout(PROVIDER_SEARCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .exceptionally(ex -> {
                            log.error("Provider timed out or failed unexpectedly: {}", ex.getMessage());
                            return List.of();
                        }))
                .toList();

        List<PropertyOffer> allOffers = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();

        return harmonizer.harmonize(allOffers);
    }
}