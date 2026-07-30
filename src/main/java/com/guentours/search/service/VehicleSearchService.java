package com.guentours.search.service;

import com.guentours.provider.*;
import com.guentours.search.*;
import com.guentours.search.domain.HarmonizedVehicleOffer;
import com.guentours.search.domain.VehicleHarmonizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class VehicleSearchService {

    private static final Logger log = LoggerFactory.getLogger(VehicleSearchService.class);
    private static final int PROVIDER_SEARCH_TIMEOUT_SECONDS = 30;

    private final Map<ProviderType, TravelProviderClient> providerClientsMap;
    private final List<TravelProviderClient> providerClients;
    private final ExecutorService providerSearchExecutor;
    private final VehicleHarmonizer harmonizer;
    private final OfferCache offerCache;

    public VehicleSearchService(Map<ProviderType, TravelProviderClient> providerClientsMap,
                                List<TravelProviderClient> providerClients, ExecutorService providerSearchExecutor,
                                VehicleHarmonizer harmonizer, OfferCache offerCache) {
        this.providerClientsMap = providerClientsMap;
        this.providerClients = providerClients;
        this.providerSearchExecutor = providerSearchExecutor;
        this.harmonizer = harmonizer;
        this.offerCache = offerCache;
    }

    public List<HarmonizedVehicleOffer> search(VehicleSearchCriteria request) {
        VehicleSearchCriteria criteria = new VehicleSearchCriteria(
                request.pickupCity().toUpperCase(),
                request.dropoffCity() == null ? null : request.dropoffCity().toUpperCase(),
                request.rentalStart(),
                request.pickupTime(),
                request.rentalEnd(),
                request.dropoffTime(),
                request.category(),
                request.withDriver(),
                request.driverAge25Plus(),
                request.currency() == null ? "XAF" : request.currency()
        );

        List<CompletableFuture<List<VehicleOffer>>> futures = providerClients.stream()
                .filter(TravelProviderClient::isEnabled)
                .map(client -> CompletableFuture.supplyAsync(() -> {
                            log.info("Dispatching vehicle search to provider {}", client.getType());
                            try {
                                List<VehicleOffer> offers = client.searchVehicles(criteria);
                                log.info("Provider {} returned {} offers", client.getType(), offers != null ? offers.size() : 0);
                                return offers != null ? offers : List.<VehicleOffer>of();
                            } catch (Exception e) {
                                log.error("Error executing vehicle search on provider {}: {}", client.getType(), e.getMessage(), e);
                                return List.<VehicleOffer>of();
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

        List<VehicleOffer> allOffers = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();

        return harmonizer.harmonize(allOffers);
    }
}