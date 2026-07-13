package com.guentours.provider.sabre;

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
 * Adapter for Sabre's Dev Studio REST APIs (https://developer.sabre.com). Real
 * integration requires an OAuth2 client-credentials token exchange against
 * {@code /v2/auth/token} before calling the Bargain Finder Max / Hotel search
 * endpoints - wire that up in {@link #callFlightApi}/{@link #callHotelApi}
 * once sandbox credentials are available.
 */
@Component
public class SabreClient implements TravelProviderClient {

    private static final Logger log = LoggerFactory.getLogger(SabreClient.class);
    private static final List<String> AIRLINES = List.of("AF", "DL", "SB");
    private static final List<String> HOTELS = List.of("Hotel Le Meridien", "Ibis Central", "Sabre Grand Hotel");

    private final ProviderProperties.Vendor config;
    private final WebClient webClient;

    public SabreClient(WebClient.Builder webClientBuilder, ProviderProperties properties) {
        this.config = properties.getSabre();
        this.webClient = webClientBuilder.baseUrl(
                config.getBaseUrl().isBlank() ? "https://api.sabre.com" : config.getBaseUrl()).build();
    }

    @Override
    public ProviderType getType() {
        return ProviderType.SABRE;
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
            return config.isMockMode() ? ProviderMockSupport.flights(getType(), criteria, AIRLINES, 1.05)
                    : callFlightApi(criteria);
        } catch (Exception ex) {
            log.warn("Sabre flight search failed, skipping this provider: {}", ex.getMessage());
            return List.of();
        }
    }

    @Override
    public List<HotelOffer> searchHotels(HotelSearchCriteria criteria) {
        if (!isEnabled()) {
            return List.of();
        }
        try {
            return config.isMockMode() ? ProviderMockSupport.hotels(getType(), criteria, HOTELS, 1.03)
                    : callHotelApi(criteria);
        } catch (Exception ex) {
            log.warn("Sabre hotel search failed, skipping this provider: {}", ex.getMessage());
            return List.of();
        }
    }

    @Override
    public FlightPriceVerification verifyFlightPrice(String flightOfferId) {
        if (config.isMockMode()) {
            return ProviderMockSupport.verifyFlightPrice(flightOfferId);
        }
        throw new ProviderException("Sabre live flight price verification is not yet integrated");
    }

    @Override
    public HotelPriceVerification verifyHotelPrice(String hotelOfferId) {
        if (config.isMockMode()) {
            return ProviderMockSupport.verifyHotelPrice(hotelOfferId);
        }
        throw new ProviderException("Sabre live hotel price verification is not yet integrated");
    }

    @Override
    public ProviderBookingConfirmation createFlightHold(FlightBookingRequest request) {
        if (config.isMockMode()) {
            return ProviderMockSupport.flightHold(getType());
        }
        throw new ProviderException("Sabre live flight booking is not yet integrated");
    }

    @Override
    public ProviderBookingConfirmation createHotelHold(HotelBookingRequest request) {
        if (config.isMockMode()) {
            return ProviderMockSupport.hotelHold(getType());
        }
        throw new ProviderException("Sabre live hotel booking is not yet integrated");
    }

    @Override
    public FinalTicketConfirmation issueFlightTicket(String pnrCode, PaymentDetails payment) {
        if (config.isMockMode()) {
            return ProviderMockSupport.issueFlightTicket(getType(), pnrCode, 1);
        }
        throw new ProviderException("Sabre live flight ticketing is not yet integrated");
    }

    @Override
    public FinalHotelConfirmation confirmHotelBooking(String hotelBookingRef, PaymentDetails payment) {
        if (config.isMockMode()) {
            return ProviderMockSupport.confirmHotelBooking(getType(), hotelBookingRef);
        }
        throw new ProviderException("Sabre live hotel booking is not yet integrated");
    }

    @Override
    public void cancelFlightBooking(String pnrCode) {
        if (config.isMockMode()) {
            log.info("Mock-cancelled Sabre flight PNR {}", pnrCode);
            return;
        }
        throw new ProviderException("Sabre live flight cancellation is not yet integrated");
    }

    @Override
    public void cancelHotelBooking(String hotelBookingRef) {
        if (config.isMockMode()) {
            log.info("Mock-cancelled Sabre hotel booking {}", hotelBookingRef);
            return;
        }
        throw new ProviderException("Sabre live hotel cancellation is not yet integrated");
    }

    private List<FlightOffer> callFlightApi(FlightSearchCriteria criteria) {
        // TODO OAuth2 token exchange + Bargain Finder Max call, mapped onto FlightOffer via this.webClient.
        throw new ProviderException("Sabre live flight search is not yet integrated");
    }

    private List<HotelOffer> callHotelApi(HotelSearchCriteria criteria) {
        // TODO Sabre Hotel Search v3 call, mapped onto HotelOffer via this.webClient.
        throw new ProviderException("Sabre live hotel search is not yet integrated");
    }
}
