package com.guentours.search;

import com.guentours.provider.FinalHotelConfirmation;
import com.guentours.provider.FinalTicketConfirmation;
import com.guentours.provider.FlightBookingRequest;
import com.guentours.provider.FlightOffer;
import com.guentours.provider.FlightSearchCriteria;
import com.guentours.provider.HotelBookingRequest;
import com.guentours.provider.HotelOffer;
import com.guentours.provider.HotelSearchCriteria;
import com.guentours.provider.PaymentDetails;
import com.guentours.provider.ProviderBookingConfirmation;
import com.guentours.provider.ProviderSeat;
import com.guentours.provider.ProviderSeatMap;
import com.guentours.provider.ProviderType;
import com.guentours.provider.TravelProviderClient;
import com.guentours.provider.dto.FlightPriceVerification;
import com.guentours.provider.dto.HotelPriceVerification;
import com.guentours.shared.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the seat-map SPI seam: {@link SeatMapService} uses the offer provider's real seat map
 * when it exposes one, and falls back to the generic simulated 15x6 map otherwise.
 */
class SeatMapServiceTest {

    private String cache(OfferCache offerCache) {
        FlightOffer offer = new FlightOffer(ProviderType.SABRE, "SABRE-AF123-2026-09-01", "AF", "AF123",
                "CDG", "JFK", LocalDateTime.of(2026, 9, 1, 10, 0), LocalDateTime.of(2026, 9, 1, 13, 0),
                "ECONOMY", new Money(BigDecimal.valueOf(500), "EUR"), 9);
        return offerCache.cacheFlightOffer(offer);
    }

    @Test
    void usesTheProviderSeatMapWhenAvailable() {
        OfferCache offerCache = new OfferCache();
        String offerId = cache(offerCache);
        var providerMap = new ProviderSeatMap(2, List.of("A", "B"), List.of(
                new ProviderSeat("1A", true, null),
                new ProviderSeat("1B", false, new Money(BigDecimal.valueOf(25), "EUR")),
                new ProviderSeat("2A", true, null),
                new ProviderSeat("2B", true, null)));
        SeatMapService service = new SeatMapService(offerCache, List.of(new StubProvider(providerMap)));

        SeatMapResponse response = service.seatMapFor(offerId);

        assertThat(response.rows()).isEqualTo(2);
        assertThat(response.columns()).containsExactly("A", "B");
        assertThat(response.seats()).hasSize(4);
        assertThat(response.seats().get(0).seatNumber()).isEqualTo("1A");
        assertThat(response.seats().get(1).available()).isFalse();
    }

    @Test
    void fallsBackToTheSimulatedMapWhenProviderExposesNone() {
        OfferCache offerCache = new OfferCache();
        String offerId = cache(offerCache);
        SeatMapService service = new SeatMapService(offerCache, List.of(new StubProvider(null)));

        SeatMapResponse response = service.seatMapFor(offerId);

        assertThat(response.rows()).isEqualTo(15);
        assertThat(response.columns()).hasSize(6);
        assertThat(response.seats()).hasSize(15 * 6);
        assertThat(response.seats().get(0).seatNumber()).isEqualTo("1A");
    }

    /** Minimal SABRE provider stub whose only meaningful behaviour is the seat map. */
    private record StubProvider(ProviderSeatMap seatMap) implements TravelProviderClient {
        @Override
        public ProviderType getType() {
            return ProviderType.SABRE;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public List<FlightOffer> searchFlights(FlightSearchCriteria criteria) {
            return List.of();
        }

        @Override
        public List<HotelOffer> searchHotels(HotelSearchCriteria criteria) {
            return List.of();
        }

        @Override
        public ProviderSeatMap seatMap(FlightOffer offer) {
            return seatMap;
        }

        @Override
        public FlightPriceVerification verifyFlightPrice(FlightOffer offer) {
            return null;
        }

        @Override
        public HotelPriceVerification verifyHotelPrice(HotelOffer offer) {
            return null;
        }

        @Override
        public ProviderBookingConfirmation createFlightHold(FlightBookingRequest request) {
            return null;
        }

        @Override
        public ProviderBookingConfirmation createHotelHold(HotelBookingRequest request) {
            return null;
        }

        @Override
        public FinalTicketConfirmation issueFlightTicket(String pnrCode, PaymentDetails payment) {
            return null;
        }

        @Override
        public FinalHotelConfirmation confirmHotelBooking(String hotelBookingRef, PaymentDetails payment) {
            return null;
        }

        @Override
        public void cancelFlightBooking(String pnrCode) {
        }

        @Override
        public void cancelHotelBooking(String hotelBookingRef) {
        }
    }
}
