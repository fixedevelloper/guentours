package com.guentours.search;

import com.guentours.provider.FlightOffer;
import com.guentours.provider.ProviderSeatMap;
import com.guentours.provider.ProviderType;
import com.guentours.provider.TravelProviderClient;
import com.guentours.shared.exception.NotFoundException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Serves the seat map for the seat-selection step at flight checkout. It first asks the offer's
 * own provider for a real seat map via {@link TravelProviderClient#seatMap}; when the provider
 * exposes none (the default), it falls back to a deterministic simulated map derived from the
 * flight's own attributes (same approach {@code ProviderMockSupport} uses for prices), so repeated
 * lookups for the same physical flight always return the same layout.
 */
@Component
class SeatMapService {

    private static final List<String> COLUMNS = List.of("A", "B", "C", "D", "E", "F");
    private static final int ROWS = 15;
    private static final int OCCUPIED_PERCENT = 20;

    private final OfferCache offerCache;
    private final Map<ProviderType, TravelProviderClient> providerClients;

    SeatMapService(OfferCache offerCache, List<TravelProviderClient> providerClients) {
        this.offerCache = offerCache;
        this.providerClients = providerClients.stream()
                .collect(Collectors.toMap(TravelProviderClient::getType, Function.identity()));
    }

    SeatMapResponse seatMapFor(String offerId) {
        FlightOffer offer = offerCache.getFlightOffer(offerId)
                .orElseThrow(() -> new NotFoundException("This flight offer has expired, please search again"));

        SeatMapResponse providerMap = fromProvider(offer);
        return providerMap != null ? providerMap : simulate(offer);
    }

    /** Real seat map from the offer's provider, or null when the provider exposes none. */
    private SeatMapResponse fromProvider(FlightOffer offer) {
        TravelProviderClient client = providerClients.get(offer.providerType());
        if (client == null) {
            return null;
        }
        ProviderSeatMap map = client.seatMap(offer);
        if (map == null || map.seats() == null || map.seats().isEmpty()) {
            return null;
        }
        List<Seat> seats = map.seats().stream()
                .map(seat -> new Seat(seat.seatNumber(), seat.available()))
                .toList();
        return new SeatMapResponse(map.rows(), map.columns(), seats);
    }

    private SeatMapResponse simulate(FlightOffer offer) {
        Random random = new Random(("%s|%s|%s|%s".formatted(
                offer.airline(), offer.flightNumber(), offer.departureTime(), offer.cabinClass())).hashCode());

        List<Seat> seats = new ArrayList<>();
        for (int row = 1; row <= ROWS; row++) {
            for (String column : COLUMNS) {
                boolean occupied = random.nextInt(100) < OCCUPIED_PERCENT;
                seats.add(new Seat(row + column, !occupied));
            }
        }
        return new SeatMapResponse(ROWS, COLUMNS, seats);
    }
}
