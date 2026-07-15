package com.guentours.provider.travelopro;

import com.guentours.provider.FinalHotelConfirmation;
import com.guentours.provider.FinalTicketConfirmation;
import com.guentours.provider.FlightBookingRequest;
import com.guentours.provider.FlightOffer;
import com.guentours.provider.FlightSearchCriteria;
import com.guentours.provider.HotelBookingRequest;
import com.guentours.provider.HotelOffer;
import com.guentours.provider.HotelSearchCriteria;
import com.guentours.provider.JourneyType;
import com.guentours.provider.PassengerInfo;
import com.guentours.provider.PaymentDetails;
import com.guentours.provider.ProviderBookingConfirmation;
import com.guentours.provider.ProviderMockSupport;
import com.guentours.provider.ProviderProperties;
import com.guentours.provider.ProviderType;
import com.guentours.provider.TravelProviderClient;
import com.guentours.provider.dto.FlightPriceVerification;
import com.guentours.provider.dto.HotelPriceVerification;
import com.guentours.shared.Money;
import com.guentours.shared.exception.ProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Adapter for the Travelopro/TBO travel API (https://travelnext.works). When
 * {@code app.providers.travelopro.mock-mode=false} and a base URL/credentials are
 * configured, {@link #callFlightApi} / {@link #callHotelApi} below are the
 * integration points to map Travelopro's XML/JSON request-response contract
 * onto our canonical {@link FlightOffer}/{@link HotelOffer} shapes.
 */
@Component
public class TraveloproClient implements TravelProviderClient {

    private static final Logger log = LoggerFactory.getLogger(TraveloproClient.class);
    private static final List<String> AIRLINES = List.of("AF", "DL", "TO");
    private static final List<String> HOTELS = List.of("Hotel Le Meridien", "Ibis Central", "Travelopro Suites");

    private final ProviderProperties.Vendor config;
    private final RestClient restClient;

    public TraveloproClient(RestClient.Builder restClientBuilder, ProviderProperties properties) {
        this.config = properties.getTravelopro();
        ClientHttpRequestFactorySettings timeoutSettings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(config.getTimeoutMillis()))
                .withReadTimeout(Duration.ofMillis(config.getTimeoutMillis()));
        this.restClient = restClientBuilder
                .baseUrl(config.getBaseUrl().isBlank() ? "https://travelnext.works" : config.getBaseUrl())
                .requestFactory(ClientHttpRequestFactories.get(timeoutSettings))
                .build();
    }

    @Override
    public ProviderType getType() {
        return ProviderType.TRAVELOPRO;
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
            return config.isMockMode() ? ProviderMockSupport.flights(getType(), criteria, AIRLINES, 0.95)
                    : callFlightApi(criteria);
        } catch (Exception ex) {
            log.warn("Travelopro flight search failed, skipping this provider: {}", ex.getMessage());
            return List.of();
        }
    }

    @Override
    public List<HotelOffer> searchHotels(HotelSearchCriteria criteria) {
        if (!isEnabled()) {
            return List.of();
        }
        try {
            return config.isMockMode() ? ProviderMockSupport.hotels(getType(), criteria, HOTELS, 0.97)
                    : callHotelApi(criteria);
        } catch (Exception ex) {
            log.warn("Travelopro hotel search failed, skipping this provider: {}", ex.getMessage());
            return List.of();
        }
    }

    @Override
    public FlightPriceVerification verifyFlightPrice(FlightOffer offer) {
        if (config.isMockMode()) {
            return ProviderMockSupport.verifyFlightPrice(offer.providerOfferId());
        }
        throw new ProviderException("Travelopro live flight price verification is not yet integrated");
    }

    @Override
    public HotelPriceVerification verifyHotelPrice(HotelOffer offer) {
        if (config.isMockMode()) {
            return ProviderMockSupport.verifyHotelPrice(offer.providerOfferId());
        }
        throw new ProviderException("Travelopro live hotel price verification is not yet integrated");
    }

    @Override
    public ProviderBookingConfirmation createFlightHold(FlightBookingRequest request) {
        if (config.isMockMode()) {
            return ProviderMockSupport.flightHold(getType());
        }
        return callBookApi(request);
    }

    @Override
    public ProviderBookingConfirmation createHotelHold(HotelBookingRequest request) {
        if (config.isMockMode()) {
            return ProviderMockSupport.hotelHold(getType());
        }
        throw new ProviderException("Travelopro live hotel booking is not yet integrated");
    }

    @Override
    public FinalTicketConfirmation issueFlightTicket(String pnrCode, PaymentDetails payment) {
        if (config.isMockMode()) {
            return ProviderMockSupport.issueFlightTicket(getType(), pnrCode, 1);
        }
        return callTicketApi(pnrCode);
    }

    @Override
    public FinalHotelConfirmation confirmHotelBooking(String hotelBookingRef, PaymentDetails payment) {
        if (config.isMockMode()) {
            return ProviderMockSupport.confirmHotelBooking(getType(), hotelBookingRef);
        }
        throw new ProviderException("Travelopro live hotel booking is not yet integrated");
    }

    @Override
    public void cancelFlightBooking(String pnrCode) {
        if (config.isMockMode()) {
            log.info("Mock-cancelled Travelopro flight PNR {}", pnrCode);
            return;
        }
        throw new ProviderException("Travelopro live flight cancellation is not yet integrated");
    }

    @Override
    public void cancelHotelBooking(String hotelBookingRef) {
        if (config.isMockMode()) {
            log.info("Mock-cancelled Travelopro hotel booking {}", hotelBookingRef);
            return;
        }
        throw new ProviderException("Travelopro live hotel cancellation is not yet integrated");
    }

    private List<FlightOffer> callFlightApi(FlightSearchCriteria criteria) {
        TraveloproAvailabilityRequest request = buildAvailabilityRequest(criteria);

        TraveloproAvailabilityResponse response = restClient.post()
                .uri("/api/aeroVE5/availability")
                .body(request)
                .retrieve()
                .body(TraveloproAvailabilityResponse.class);

        log.info(response.toString());
        var result = response == null || response.AirSearchResponse() == null
                ? null : response.AirSearchResponse().AirSearchResult();
        if (result == null || result.FareItineraries() == null) {
            return List.of();
        }

        return result.FareItineraries().stream()
                .map(TraveloproAvailabilityResponse.FareItineraryWrapper::FareItinerary)
                .map(itinerary -> toFlightOffer(itinerary, criteria))
                .filter(Objects::nonNull)
                .toList();
    }

    private TraveloproAvailabilityRequest buildAvailabilityRequest(FlightSearchCriteria criteria) {
        String journeyType = switch (criteria.journeyType()) {
            case ONE_WAY -> "OneWay";
            case ROUND_TRIP -> "Return";
            case MULTI_CITY -> throw new ProviderException("Travelopro multi-city search is not supported yet");
        };

        var segment = new TraveloproAvailabilityRequest.OriginDestinationInfoEntry(
                criteria.departureDate().toString(),
                criteria.journeyType() == JourneyType.ROUND_TRIP ? criteria.returnDate().toString() : null,
                criteria.origin(),
                criteria.destination());

        return new TraveloproAvailabilityRequest(
                config.getUsername(),
                config.getPassword(),
                config.getAccessMode(),
                config.getClientIp(),
                criteria.currency(),
                journeyType,
                List.of(segment),
                toTraveloproCabinClass(criteria.cabinClass()),
                criteria.adults(),
                criteria.children(),
                criteria.infants());
    }

    private FlightOffer toFlightOffer(TraveloproAvailabilityResponse.FareItinerary itinerary, FlightSearchCriteria criteria) {
        if (itinerary == null || itinerary.AirItineraryFareInfo() == null
                || itinerary.AirItineraryFareInfo().ItinTotalFares() == null
                || itinerary.OriginDestinationOptions() == null) {
            return null;
        }

        List<TraveloproAvailabilityResponse.SegmentWrapper> segments = itinerary.OriginDestinationOptions().stream()
                .flatMap(option -> option.OriginDestinationOption() == null
                        ? Stream.<TraveloproAvailabilityResponse.SegmentWrapper>empty()
                        : option.OriginDestinationOption().stream())
                .toList();
        if (segments.isEmpty()) {
            return null;
        }

        var first = segments.get(0).FlightSegment();
        var last = segments.get(segments.size() - 1).FlightSegment();
        if (first == null || last == null) {
            return null;
        }

        int seats = segments.stream()
                .map(TraveloproAvailabilityResponse.SegmentWrapper::SeatsRemaining)
                .filter(Objects::nonNull)
                .map(TraveloproAvailabilityResponse.SeatsRemaining::Number)
                .filter(Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(0);

        var totalFare = itinerary.AirItineraryFareInfo().ItinTotalFares().TotalFare();
        Money price = new Money(new BigDecimal(totalFare.Amount()), totalFare.CurrencyCode());

        return new FlightOffer(
                getType(),
                itinerary.AirItineraryFareInfo().FareSourceCode(),
                first.MarketingAirlineCode(),
                first.MarketingAirlineCode() + first.FlightNumber(),
                first.DepartureAirportLocationCode(),
                last.ArrivalAirportLocationCode(),
                LocalDateTime.parse(first.DepartureDateTime()),
                LocalDateTime.parse(last.ArrivalDateTime()),
                criteria.cabinClass(),
                price,
                seats);
    }

    private String toTraveloproCabinClass(String cabinClass) {
        if (cabinClass == null) {
            return "Economy";
        }
        return switch (cabinClass.toUpperCase()) {
            case "BUSINESS" -> "Business";
            case "FIRST" -> "First";
            case "PREMIUM_ECONOMY" -> "Premium Economy";
            default -> "Economy";
        };
    }

    private List<HotelOffer> callHotelApi(HotelSearchCriteria criteria) {
        // TODO integrate Travelopro's hotel search endpoint; map its response onto HotelOffer here.
        throw new ProviderException("Travelopro live hotel search is not yet integrated");
    }

    /**
     * Holds the fare quoted at search time ({@code FareSourceCode}) against a PNR without
     * ticketing it yet - {@link #issueFlightTicket} converts the hold into e-tickets once
     * payment has cleared. Travelopro's Book response doesn't carry an explicit hold
     * expiry in this best-effort contract, so a conservative 30-minute policy default is
     * applied here; tighten it once the real Book API docs confirm an actual deadline field.
     */
    private ProviderBookingConfirmation callBookApi(FlightBookingRequest request) {
        TraveloproBookRequest bookRequest = buildBookRequest(request);
        TraveloproBookResponse bookResponse = restClient.post()
                .uri("/api/aeroVE5/book")
                .body(bookRequest)
                .retrieve()
                .body(TraveloproBookResponse.class);

        var bookResult = bookResponse == null ? null : bookResponse.BookResult();
        if (bookResult == null || !"Success".equalsIgnoreCase(bookResult.Status())) {
            String reason = bookResult != null && bookResult.Message() != null ? bookResult.Message() : "unknown error";
            throw new ProviderException("Travelopro booking failed: " + reason);
        }

        return new ProviderBookingConfirmation(getType(), bookResult.PNR(), LocalDateTime.now().plusMinutes(30), true);
    }

    private FinalTicketConfirmation callTicketApi(String pnrCode) {
        TraveloproTicketRequest ticketRequest = new TraveloproTicketRequest(
                config.getUsername(), config.getPassword(), config.getAccessMode(), config.getClientIp(), pnrCode);

        TraveloproTicketResponse ticketResponse = restClient.post()
                .uri("/api/aeroVE5/ticket")
                .body(ticketRequest)
                .retrieve()
                .body(TraveloproTicketResponse.class);

        var ticketResult = ticketResponse == null ? null : ticketResponse.TicketResult();
        if (ticketResult == null || !"Success".equalsIgnoreCase(ticketResult.Status())) {
            String reason = ticketResult != null && ticketResult.Message() != null ? ticketResult.Message() : "unknown error";
            throw new ProviderException("Travelopro ticketing failed: " + reason);
        }

        String pnr = ticketResult.PNR() != null ? ticketResult.PNR() : pnrCode;
        List<String> ticketNumbers = ticketResult.TicketNumbers() != null ? ticketResult.TicketNumbers() : List.of();
        return new FinalTicketConfirmation(getType(), pnr, ticketNumbers, true);
    }

    private TraveloproBookRequest buildBookRequest(FlightBookingRequest request) {
        List<TraveloproBookRequest.Passenger> passengers = request.passengers().stream()
                .map(this::toTraveloproPassenger)
                .toList();
        return new TraveloproBookRequest(
                config.getUsername(),
                config.getPassword(),
                config.getAccessMode(),
                config.getClientIp(),
                request.offer().providerOfferId(),
                request.contactEmail(),
                passengers);
    }

    private TraveloproBookRequest.Passenger toTraveloproPassenger(PassengerInfo passenger) {
        String[] nameParts = splitName(passenger.fullName());
        String paxType = switch (passenger.type()) {
            case ADULT -> "Adult";
            case CHILD -> "Child";
            case INFANT -> "Infant";
        };
        return new TraveloproBookRequest.Passenger(
                "Mr",
                nameParts[0],
                nameParts[1],
                paxType,
                passenger.dateOfBirth() != null ? passenger.dateOfBirth().toString() : null,
                passenger.passportNumber());
    }

    private String[] splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new String[] {"", ""};
        }
        String trimmed = fullName.trim();
        int idx = trimmed.lastIndexOf(' ');
        if (idx < 0) {
            return new String[] {trimmed, trimmed};
        }
        return new String[] {trimmed.substring(0, idx), trimmed.substring(idx + 1)};
    }
}
