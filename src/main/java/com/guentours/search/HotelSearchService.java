package com.guentours.search;

import com.guentours.provider.HotelOffer;
import com.guentours.provider.HotelSearchCriteria;
import com.guentours.provider.TravelProviderClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
public class HotelSearchService {

    private static final Logger log = LoggerFactory.getLogger(HotelSearchService.class);

    private final List<TravelProviderClient> providerClients;
    private final ExecutorService providerSearchExecutor;
    private final HotelHarmonizer harmonizer;

    public HotelSearchService(List<TravelProviderClient> providerClients, ExecutorService providerSearchExecutor,
                               HotelHarmonizer harmonizer) {
        this.providerClients = providerClients;
        this.providerSearchExecutor = providerSearchExecutor;
        this.harmonizer = harmonizer;
    }

    public List<HarmonizedHotelOffer> search(HotelSearchRequest request) {
        HotelSearchCriteria criteria = new HotelSearchCriteria(
                request.cityCode().toUpperCase(), request.checkIn(), request.checkOut(),
                request.adults() == null ? 1 : request.adults(),
                request.rooms() == null ? 1 : request.rooms());

        List<CompletableFuture<List<HotelOffer>>> futures = providerClients.stream()
                .filter(TravelProviderClient::isEnabled)
                .map(client -> CompletableFuture.supplyAsync(() -> {
                    log.info("Dispatching hotel search to provider {}", client.getType());
                    return client.searchHotels(criteria);
                }, providerSearchExecutor))
                .toList();

        List<HotelOffer> allOffers = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .toList();

        return harmonizer.harmonize(allOffers);
    }
}
