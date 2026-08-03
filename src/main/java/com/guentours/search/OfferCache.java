package com.guentours.search;

import com.guentours.provider.FlightOffer;
import com.guentours.provider.HotelOffer;
import com.guentours.provider.HotelSearchCriteria;
import com.guentours.provider.ProviderType;
import com.guentours.provider.PropertyOffer;
import com.guentours.provider.VehicleOffer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the exact provider offers returned by a search for a short window so that
 * checkout resolves prices/details server-side from an opaque {@code offerId}
 * instead of trusting whatever price the client sends back.
 */
@Component
public class OfferCache {

    private static final long TTL_MILLIS = 20L * 60 * 1000;

    private record Entry<T>(T value, long expiresAtEpochMilli) {
        boolean isExpired() {
            return Instant.now().toEpochMilli() > expiresAtEpochMilli;
        }
    }

    private final Map<String, Entry<FlightOffer>> flightOffers = new ConcurrentHashMap<>();
    private final Map<String, Entry<HotelOffer>> hotelOffers = new ConcurrentHashMap<>();
    private final Map<String, Entry<VehicleOffer>> vehicleOffers = new ConcurrentHashMap<>();

    public String cacheFlightOffer(FlightOffer offer) {
        String id = UUID.randomUUID().toString();
        flightOffers.put(id, new Entry<>(offer, Instant.now().toEpochMilli() + TTL_MILLIS));
        return id;
    }

    public String cacheHotelOffer(HotelOffer offer) {
        String id = UUID.randomUUID().toString();
        hotelOffers.put(id, new Entry<>(offer, Instant.now().toEpochMilli() + TTL_MILLIS));
        return id;
    }

    public String cacheVehicleOffer(VehicleOffer offer) {
        String id = UUID.randomUUID().toString();
        vehicleOffers.put(id, new Entry<>(offer, Instant.now().toEpochMilli() + TTL_MILLIS));
        return id;
    }

    public Optional<FlightOffer> getFlightOffer(String offerId) {
        Entry<FlightOffer> entry = flightOffers.get(offerId);
        if (entry == null || entry.isExpired()) {
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    public Optional<HotelOffer> getHotelOffer(String offerId) {
        Entry<HotelOffer> entry = hotelOffers.get(offerId);
        if (entry == null || entry.isExpired()) {
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    public Optional<VehicleOffer> getVehicleOffer(String offerId) {
        Entry<VehicleOffer> entry = vehicleOffers.get(offerId);
        if (entry == null || entry.isExpired()) {
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }
    private final Map<String, Entry<PropertyOffer>> propertyOffers = new ConcurrentHashMap<>();

    public String cachePropertyOffer(PropertyOffer offer) {
        String id = UUID.randomUUID().toString();
        propertyOffers.put(id, new Entry<>(offer, Instant.now().toEpochMilli() + TTL_MILLIS));
        return id;
    }

    public Optional<PropertyOffer> getPropertyOffer(String offerId) {
        Entry<PropertyOffer> entry = propertyOffers.get(offerId);
        if (entry == null || entry.isExpired()) {
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }
    /**
     * One hotel search's original criteria plus whichever providers captured a pagination token
     * on their page-1 offers (see {@code HotelOffer#context}, key {@code "searchIdentifier"}) -
     * lets {@code HotelSearchService#loadMore} re-run only those providers for page N without the
     * frontend ever needing to know criteria or provider-specific tokens.
     */
    public record HotelSearchSession(HotelSearchCriteria criteria, Map<ProviderType, String> providerSearchIdentifiers) {
    }

    private final Map<String, Entry<HotelSearchSession>> hotelSearchSessions = new ConcurrentHashMap<>();

    public String cacheHotelSearchSession(HotelSearchCriteria criteria, Map<ProviderType, String> providerSearchIdentifiers) {
        String id = UUID.randomUUID().toString();
        hotelSearchSessions.put(id, new Entry<>(new HotelSearchSession(criteria, Map.copyOf(providerSearchIdentifiers)),
                Instant.now().toEpochMilli() + TTL_MILLIS));
        return id;
    }

    public Optional<HotelSearchSession> getHotelSearchSession(String searchId) {
        Entry<HotelSearchSession> entry = hotelSearchSessions.get(searchId);
        if (entry == null || entry.isExpired()) {
            return Optional.empty();
        }
        return Optional.of(entry.value());
    }

    @Scheduled(fixedRate = 5 * 60 * 1000)
    void evictExpired() {
        flightOffers.values().removeIf(Entry::isExpired);
        hotelOffers.values().removeIf(Entry::isExpired);
        vehicleOffers.values().removeIf(Entry::isExpired);
        propertyOffers.values().removeIf(Entry::isExpired);
        hotelSearchSessions.values().removeIf(Entry::isExpired);
    }
}