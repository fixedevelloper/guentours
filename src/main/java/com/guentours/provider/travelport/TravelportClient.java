package com.guentours.provider.travelport;

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
import com.guentours.provider.ProviderMockSupport;
import com.guentours.provider.ProviderProperties;
import com.guentours.provider.ProviderType;
import com.guentours.provider.TravelProviderClient;
import com.guentours.provider.dto.FlightPriceVerification;
import com.guentours.provider.dto.HotelPriceVerification;
import com.guentours.shared.exception.ProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Adapter for the Travelport JSON APIs (Universal Air/Hotel search + Air/Hotel
 * booking). Real integration requires an OAuth token from Travelport's identity
 * service before calling {@code /air/search/catalog} etc. - wire that up in
 * {@link #callFlightApi}/{@link #callHotelApi} once credentials are available.
 */
@Component
public class TravelportClient implements TravelProviderClient {

    private static final Logger log = LoggerFactory.getLogger(TravelportClient.class);
    private static final List<String> AIRLINES = List.of("AF", "DL", "TP");
    private static final List<String> HOTELS = List.of("Hotel Le Meridien", "Ibis Central", "Travelport Resort");

    private final ProviderProperties.Vendor config;
    private final WebClient webClient;

    public TravelportClient(WebClient.Builder webClientBuilder, ProviderProperties properties) {
        this.config = properties.getTravelport();
        this.webClient = webClientBuilder.baseUrl(
                config.getBaseUrl().isBlank() ? "https://api.travelport.com" : config.getBaseUrl()).build();
    }

    @Override
    public ProviderType getType() {
        return ProviderType.TRAVELPORT;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    @Override
    public List<FlightOffer> searchFlights(FlightSearchCriteria criteria) {
        if (!isEnabled()) {
            return List.of();
        }
        try {
            return config.isMockMode() ? ProviderMockSupport.flights(getType(), criteria, AIRLINES, 1.0)
                    : callFlightApi(criteria);
        } catch (Exception ex) {
            log.warn("Travelport flight search failed, skipping this provider: {}", ex.getMessage());
            return List.of();
        }
    }

    @Override
    public List<HotelOffer> searchHotels(HotelSearchCriteria criteria) {
        if (!isEnabled()) {
            return List.of();
        }
        try {
            return config.isMockMode() ? ProviderMockSupport.hotels(getType(), criteria, HOTELS, 1.0)
                    : callHotelApi(criteria);
        } catch (Exception ex) {
            log.warn("Travelport hotel search failed, skipping this provider: {}", ex.getMessage());
            return List.of();
        }
    }

    @Override
    public FlightPriceVerification verifyFlightPrice(String flightOfferId) {
        if (config.isMockMode()) {
            return ProviderMockSupport.verifyFlightPrice(flightOfferId);
        }
        throw new ProviderException("Travelport live flight price verification is not yet integrated");
    }

    @Override
    public HotelPriceVerification verifyHotelPrice(String hotelOfferId) {
        if (config.isMockMode()) {
            return ProviderMockSupport.verifyHotelPrice(hotelOfferId);
        }
        throw new ProviderException("Travelport live hotel price verification is not yet integrated");
    }

    @Override
    public ProviderBookingConfirmation createFlightHold(FlightBookingRequest request) {
        if (config.isMockMode()) {
            return ProviderMockSupport.flightHold(getType());
        }
        throw new ProviderException("Travelport live flight booking is not yet integrated");
    }

    @Override
    public ProviderBookingConfirmation createHotelHold(HotelBookingRequest request) {
        if (config.isMockMode()) {
            return ProviderMockSupport.hotelHold(getType());
        }
        throw new ProviderException("Travelport live hotel booking is not yet integrated");
    }

    @Override
    public FinalTicketConfirmation issueFlightTicket(String pnrCode, PaymentDetails payment) {
        if (config.isMockMode()) {
            return ProviderMockSupport.issueFlightTicket(getType(), pnrCode, 1);
        }
        throw new ProviderException("Travelport live flight ticketing is not yet integrated");
    }

    @Override
    public FinalHotelConfirmation confirmHotelBooking(String hotelBookingRef, PaymentDetails payment) {
        if (config.isMockMode()) {
            return ProviderMockSupport.confirmHotelBooking(getType(), hotelBookingRef);
        }
        throw new ProviderException("Travelport live hotel booking is not yet integrated");
    }

    @Override
    public void cancelFlightBooking(String pnrCode) {
        if (config.isMockMode()) {
            log.info("Mock-cancelled Travelport flight PNR {}", pnrCode);
            return;
        }
        throw new ProviderException("Travelport live flight cancellation is not yet integrated");
    }

    @Override
    public void cancelHotelBooking(String hotelBookingRef) {
        if (config.isMockMode()) {
            log.info("Mock-cancelled Travelport hotel booking {}", hotelBookingRef);
            return;
        }
        throw new ProviderException("Travelport live hotel cancellation is not yet integrated");
    }

    private List<FlightOffer> callFlightApi(FlightSearchCriteria criteria) {
        // TODO Travelport OAuth token exchange + Universal Air search, mapped onto FlightOffer via this.webClient.
        throw new ProviderException("Travelport live flight search is not yet integrated");
    }

    private List<HotelOffer> callHotelApi(HotelSearchCriteria criteria) {
        // TODO Travelport Universal Hotel search, mapped onto HotelOffer via this.webClient.
        throw new ProviderException("Travelport live hotel search is not yet integrated");
    }
}
