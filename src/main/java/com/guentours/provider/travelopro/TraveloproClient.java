package com.guentours.provider.travelopro;

import com.guentours.provider.*;
import com.guentours.provider.dto.FlightPriceVerification;
import com.guentours.provider.dto.HotelDetailResponse;
import com.guentours.provider.dto.HotelPriceVerification;
import com.guentours.search.OfferCache;
import com.guentours.shared.Money;
import com.guentours.shared.exception.ProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;
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
    private static final List<String> HOTELS = List.of("Hotel Le Meridien", "Ibis Central");

    // TravelNext hotel search wants a currency/nationality/radius our HotelSearchCriteria doesn't
    // carry yet; these defaults keep results in the same currency the other adapters quote (EUR)
    // until the customer's currency is threaded through the search criteria.
    private static final String HOTEL_CURRENCY = "EUR";
    private static final String HOTEL_NATIONALITY = "US";
    private static final int HOTEL_SEARCH_RADIUS_KM = 20;
    private static final int HOTEL_MAX_RESULTS = 30;

    private final ProviderProperties.Vendor config;
    private final RestClient restClient;

    public TraveloproClient(RestClient.Builder restClientBuilder, ProviderProperties properties) {
        this.config = properties.getTravelopro();

        // Configuration des timeouts distincts (par exemple 10s pour se connecter, 35s pour lire la réponse)
        long connectTimeout = config.getTimeoutMillis() > 0 ? config.getTimeoutMillis() : 10_000L;
        long readTimeout = config.getTimeoutMillis() > 0 ? config.getTimeoutMillis() : 35_000L;

        ClientHttpRequestFactorySettings timeoutSettings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(connectTimeout))
                .withReadTimeout(Duration.ofMillis(readTimeout));

        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setSupportedMediaTypes(List.of(
                MediaType.APPLICATION_JSON,
                MediaType.APPLICATION_OCTET_STREAM,
                new MediaType("application", "*+json")
        ));

        this.restClient = restClientBuilder
                .baseUrl(config.getBaseUrl().isBlank() ? "https://travelnext.works" : config.getBaseUrl())
                .requestFactory(ClientHttpRequestFactories.get(timeoutSettings))
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64)") // Évite d'être bloqué par un WAF/Cloudflare
                .messageConverters(converters -> {
                    converters.clear();
                    converters.add(jsonConverter);
                })
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
        log.info(criteria.toString());
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
        return callFlightPriceVerification(offer);
    }

    @Override
    public HotelPriceVerification verifyHotelPrice(HotelOffer offer) {
        if (config.isMockMode()) {
            return ProviderMockSupport.verifyHotelPrice(offer.providerOfferId());
        }
        return callRoomRatesPriceCheck(offer);
    }

    @Override
    public HotelDetail getDetailHotel(HotelOffer offer) {
        if (config.isMockMode()) {
            return null;
        }
        return callHotelContentAPI(offer);
    }

    @Override
    public List<RoomOffer> getRoomOffers(HotelOffer offer) {
        if (config.isMockMode()) {
            log.info("[Travelopro] Mode MOCK actif pour l'offre : {}", offer);
            return List.of();
        }

        String sessionId = offer.context("sessionId");
        String hotelId = offer.context("hotelId");
        String productId = offer.context("productId");
        String tokenId = offer.context("tokenId");

        log.info("[Travelopro] SessionId: {}, TokenId: {}", sessionId, tokenId);

        // Validation défensive des paramètres requis
        if (sessionId == null || hotelId == null) {
            log.warn("[Travelopro] Paramètres de contexte manquants pour getRoomOffers (sessionId={}, hotelId={})", sessionId, hotelId);
            return List.of();
        }

        // Construction du payload POST
        TraveloproRoomRateRequest requestBody = new TraveloproRoomRateRequest(
                sessionId,
                hotelId,
                productId,
                tokenId
        );

        try {
            TraveloproRoomRateResponse response = restClient.post()
                    .uri("/api/hotel-api-v6/get_room_rates")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(TraveloproRoomRateResponse.class);

            log.debug("[Travelopro] Réponse reçue pour hotelId={} : {}", hotelId, response);

            if (response == null) {
                log.warn("[Travelopro] Réponse nulle reçue pour hotelId={}", hotelId);
                return List.of();
            }

            log.info("[Travelopro] Detail réponse : {}", response);

            List<RoomOffer> offers = response.getRoomOffers();
            if (offers.isEmpty()) {
                log.warn("[Travelopro] Aucune offre de chambre trouvée pour hotelId={}", hotelId);
            }

            return offers;

        } catch (RestClientException e) {
            log.error("[Travelopro] Erreur lors de l'appel API get_room_rates pour hotelId={}: {}", hotelId, e.getMessage(), e);
            return List.of();
        }
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
        return callHotelBookApi(request);
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
        // hotel_book already created the actual (CONFIRMED) reservation at hold time, and our own
        // PaymentGateway has since collected the funds (txn payment.transactionReference()); the
        // TravelNext hotel API has no separate ticketing/capture call, so the reservation itself
        // stands as the confirmation (same reasoning as SabreClient/TravelportClient).
        log.info("Confirmed Travelopro hotel reservation {} (charged via txn {})",
                hotelBookingRef, payment.transactionReference());
        return new FinalHotelConfirmation(getType(), hotelBookingRef, hotelBookingRef, true);
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
        // The integrated TravelNext hotel-api-v6 contract exposes booking lookup (bookingDetails)
        // but no cancellation endpoint, and its bookings are immediate/non-refundable by default;
        // log for manual follow-up rather than throwing, so an expiring-hold sweep never fails here.
        log.warn("Travelopro hotel reservation {} requires manual cancellation - hotel-api-v6 exposes "
                + "no cancel endpoint in the integrated contract", hotelBookingRef);
    }

    private HotelDetail callHotelContentAPI(HotelOffer offer) {
        // 1. Clés String en guillemets doubles "..."
        String sessionId = offer.context("sessionId");
        String hotelId = offer.context("hotelId");
        String productId =  offer.context("productId");
        String tokenId =  offer.context("tokenId");

        // 2. Construction propre de l'URL via UriBuilder (gestion automatique de l'encodage)
        HotelDetail response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/hotel-api-v6/hotelDetails")
                        .queryParam("sessionId", sessionId)
                        .queryParam("hotelId", hotelId)
                        .queryParam("productId", productId)
                        .queryParam("tokenId", tokenId)
                        .build())
                .retrieve()
                .body(HotelDetail.class);
        log.info(response.toString());
        // 3. Mapping/Retour du résultat (adapter selon la structure de HotelDetailResponse)
        return response != null ? response : null;
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

        String sessionId = response.AirSearchResponse().session_id();

        return result.FareItineraries().stream()
                .map(TraveloproAvailabilityResponse.FareItineraryWrapper::FareItinerary)
                .map(itinerary -> toFlightOffer(itinerary, criteria, sessionId))
                .filter(Objects::nonNull)
                .toList();
    }
    /**
     * Re-prices a flight offer via {@code aeroVE5/revalidate} right before a booking hold is
     * created. Requires the {@code sessionId} captured in the offer's providerContext at search
     * time - falls back to trusting the originally quoted price if that context is missing
     * (e.g. an offer that predates this capture, or a provider response that omitted it).
     */
    /**
     * Re-prices a flight offer via {@code aeroVE5/revalidate} right before a booking hold is
     * created. Requires the {@code sessionId} captured in the offer's providerContext at search
     * time - falls back to trusting the originally quoted price if that context is missing
     * (e.g. an offer that predates this capture, or a provider response that omitted it).
     */
    private FlightPriceVerification callFlightPriceVerification(FlightOffer offer) {
        String sessionId = offer.context("sessionId");
        if (sessionId == null) {
            log.warn("No sessionId in providerContext for Travelopro offer {}, skipping revalidate", offer.providerOfferId());
            return new FlightPriceVerification(offer.providerOfferId(), null, true, offer.seatsAvailable(), null);
        }

        TraveloproRevalidateRequest request = new TraveloproRevalidateRequest(sessionId, offer.providerOfferId());

        TraveloproRevalidateResponse response = restClient.post()
                .uri("/api/aeroVE5/revalidate")
                .body(request)
                .retrieve()
                .body(TraveloproRevalidateResponse.class);

        var result = response == null || response.AirRevalidateResponse() == null
                ? null : response.AirRevalidateResponse().AirRevalidateResult();

        if (result == null || !Boolean.TRUE.equals(result.IsValid())) {
            return new FlightPriceVerification(offer.providerOfferId(), null, false, 0, null);
        }

        var fareInfo = result.FareItineraries() != null && result.FareItineraries().FareItinerary() != null
                ? result.FareItineraries().FareItinerary().AirItineraryFareInfo() : null;

        Money verifiedPrice = null;
        if (fareInfo != null && fareInfo.ItinTotalFares() != null && fareInfo.ItinTotalFares().TotalFare() != null) {
            var totalFare = fareInfo.ItinTotalFares().TotalFare();
            verifiedPrice = new Money(new BigDecimal(totalFare.Amount()), totalFare.CurrencyCode());
        }

        String baggageAllowance = extractBaggageAllowance(fareInfo);

        return new FlightPriceVerification(offer.providerOfferId(), verifiedPrice, true, offer.seatsAvailable(), baggageAllowance);
    }

    /**
     * Reads the adult passenger's baggage codes from the first matching {@code FareBreakdown} entry.
     * Travelopro reports these as short carrier-specific codes (e.g. {@code "SB"} for standard baggage)
     * rather than a parsed weight, so they're joined as-is; translating a given code to an actual kg
     * allowance would require a per-airline code table this API doesn't expose.
     */
    private String extractBaggageAllowance(TraveloproRevalidateResponse.AirItineraryFareInfo fareInfo) {
        if (fareInfo == null || fareInfo.FareBreakdown() == null) {
            return null;
        }
        return fareInfo.FareBreakdown().stream()
                .filter(entry -> entry.PassengerTypeQuantity() != null
                        && "ADT".equalsIgnoreCase(entry.PassengerTypeQuantity().Code()))
                .findFirst()
                .map(TraveloproRevalidateResponse.FareBreakdownEntry::Baggage)
                .filter(baggage -> baggage != null && !baggage.isEmpty())
                .map(baggage -> String.join(", ", baggage))
                .orElse(null);
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

    private FlightOffer toFlightOffer(TraveloproAvailabilityResponse.FareItinerary itinerary,
                                      FlightSearchCriteria criteria, String sessionId) {
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
                .map(TraveloproAvailabilityResponse.SeatsRemainingInfo::Number)
                .filter(Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(0);

        var totalFare = itinerary.AirItineraryFareInfo().ItinTotalFares().TotalFare();
        Money price = new Money(new BigDecimal(totalFare.Amount()), totalFare.CurrencyCode());

        Map<String, String> context = sessionId != null ? Map.of("sessionId", sessionId) : Map.of();

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
                seats,
                context);
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

    /**
     * Searches TravelNext's Hotel API v6 ({@code POST /api/hotel-api-v6/hotel_search}) by city name
     * and maps each returned itinerary (one hotel with its cheapest product) onto a canonical
     * {@link HotelOffer}. The {@code sessionId}/{@code productId}/{@code tokenId}/{@code hotelId}
     * needed to re-price ({@code get_room_rates}) and book ({@code hotel_book}) that exact property
     * are captured in {@code providerContext}.
     */
    private List<HotelOffer> callHotelApi(HotelSearchCriteria criteria) {
        TraveloproHotelSearchRequest request = new TraveloproHotelSearchRequest(
                config.getUsername(), config.getPassword(), config.getAccessMode(), config.getClientIp(),
                criteria.currency(), HOTEL_NATIONALITY,
                criteria.checkIn().toString(), criteria.checkOut().toString(),
                criteria.cityCode(), "",
                HOTEL_SEARCH_RADIUS_KM, HOTEL_MAX_RESULTS,
                buildOccupancy(criteria.adults(), Math.max(criteria.rooms(), 1)));

        TraveloproHotelSearchResponse response = restClient.post()
                .uri("/api/hotel-api-v6/hotel_search")
                .body(request)
                .retrieve()
                .body(TraveloproHotelSearchResponse.class);

        if (response == null || response.itineraries() == null) {
            if (response != null && response.Errors() != null) {
                log.warn("Travelopro hotel search returned an error: {} {}",
                        response.Errors().ErrorCode(), response.Errors().ErrorMessage());
            }
            return List.of();
        }


        String sessionId = response.status() != null ? response.status().sessionId() : null;
        return response.itineraries().stream()
                .map(itinerary -> toHotelOffer(itinerary, criteria, sessionId))
                .filter(Objects::nonNull)
                .toList();
    }

    private HotelOffer toHotelOffer(TraveloproHotelSearchResponse.Itinerary itinerary,
                                    HotelSearchCriteria criteria, String sessionId) {
        if (itinerary == null || itinerary.hotelId() == null || itinerary.productId() == null
                || itinerary.total() == null) {
            return null;
        }

        String currency = itinerary.currency() != null ? itinerary.currency() : HOTEL_CURRENCY;
        Map<String, String> context = new HashMap<>();
        if (sessionId != null) {
            context.put("sessionId", sessionId);
        }
        context.put("productId", itinerary.productId());
        context.put("hotelId", itinerary.hotelId());
        if (itinerary.tokenId() != null) {
            context.put("tokenId", itinerary.tokenId());
        }

        return new HotelOffer(
                getType(),
                itinerary.hotelId(),
                itinerary.hotelName(),
                itinerary.city() != null ? itinerary.city() : criteria.cityCode(),
                itinerary.propertyType() != null ? itinerary.propertyType() : "",
                criteria.checkIn(),
                criteria.checkOut(),
                new Money(BigDecimal.valueOf(itinerary.total()), currency),
                parseRating(itinerary.hotelRating()),
                context);
    }

    /** One occupancy entry per room, spreading the requested adults across the rooms (min 1 each). */
    private List<TraveloproHotelSearchRequest.Occupancy> buildOccupancy(int adults, int rooms) {
        List<TraveloproHotelSearchRequest.Occupancy> occupancy = new ArrayList<>();
        int remaining = Math.max(adults, rooms); // at least one adult per room
        for (int room = 1; room <= rooms; room++) {
            int roomsLeft = rooms - room + 1;
            int adultsHere = Math.max(1, remaining - (roomsLeft - 1));
            remaining -= adultsHere;
            occupancy.add(new TraveloproHotelSearchRequest.Occupancy(room, adultsHere, 0, List.of()));
        }
        return occupancy;
    }

    private double parseRating(String rating) {
        if (rating == null || rating.isBlank()) {
            return 0.0;
        }
        try {
            return Math.min(5.0, Double.parseDouble(rating.trim()));
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    /**
     * Re-prices an offer's hotel product via {@code get_room_rates} and reports the cheapest bookable
     * rate as the fresh price plus its cancellation policy. When the offer carries no search context
     * or the property has no bookable rate left, the originally quoted price is trusted (available).
     */
    private HotelPriceVerification callRoomRatesPriceCheck(HotelOffer offer) {
        TraveloproRoomRatesResponse.PerBookingRate rate = cheapestRate(offer);
        if (rate == null) {
            return new HotelPriceVerification(offer.providerOfferId(), null, true, null);
        }
        Money freshPrice = rate.netPrice() != null
                ? new Money(BigDecimal.valueOf(rate.netPrice()),
                        rate.currency() != null ? rate.currency() : offer.price().currency())
                : null;
        String cancellationPolicy = normalizeCancellationPolicy(rate.cancellationPolicy());
        return new HotelPriceVerification(offer.providerOfferId(), freshPrice, true, cancellationPolicy);
    }

    /**
     * Fetches the bookable room rates for the offer's hotel product ({@code get_room_rates}) and
     * returns the cheapest one - its {@code rateBasisId} is what {@code hotel_book} consumes.
     * Returns {@code null} when the offer carries no {@code productId} context or no rate is priced.
     */
    private TraveloproRoomRatesResponse.PerBookingRate cheapestRate(HotelOffer offer) {
        String productId = offer.context("productId");
        String hotelId = offer.context("hotelId");
        if (productId == null || hotelId == null) {
            return null;
        }

        TraveloproRoomRatesRequest request = new TraveloproRoomRatesRequest(
                config.getUsername(), config.getPassword(), config.getAccessMode(), config.getClientIp(),
                offer.context("sessionId"), productId, offer.context("tokenId"), hotelId);

        TraveloproRoomRatesResponse response = restClient.post()
                .uri("/api/hotel-api-v6/get_room_rates")
                .body(request)
                .retrieve()
                .body(TraveloproRoomRatesResponse.class);

        if (response == null || response.roomRates() == null
                || response.roomRates().perBookingRates() == null) {
            return null;
        }
        return response.roomRates().perBookingRates().stream()
                .filter(r -> r.rateBasisId() != null && r.netPrice() != null)
                .min((a, b) -> Double.compare(a.netPrice(), b.netPrice()))
                .orElse(null);
    }

    /**
     * Books a room via {@code hotel_book}, sending the fresh {@code rateBasisId} obtained from
     * {@link #cheapestRate} plus the guests (adults/children split by passenger type, all placed in
     * room 1 since the canonical booking request no longer carries the per-room split). No card is
     * sent - our own PaymentGateway collects the funds (see {@link #confirmHotelBooking}). TravelNext
     * hotel bookings return {@code CONFIRMED} immediately; a nominal 24h deadline is applied so the
     * booking still flows through the hold/confirm lifecycle.
     */
    private ProviderBookingConfirmation callHotelBookApi(HotelBookingRequest request) {
        TraveloproRoomRatesResponse.PerBookingRate rate = cheapestRate(request.offer());
        if (rate == null || rate.rateBasisId() == null) {
            throw new ProviderException("Travelopro hotel booking failed: no bookable rate found for "
                    + request.offer().providerOfferId());
        }

        HotelOffer offer = request.offer();
        TraveloproHotelBookRequest bookRequest = new TraveloproHotelBookRequest(
                config.getUsername(), config.getPassword(), config.getAccessMode(), config.getClientIp(),
                offer.context("sessionId"), offer.context("productId"), offer.context("tokenId"),
                rate.rateBasisId(), "GT-" + UUID.randomUUID().toString().substring(0, 8),
                request.contactEmail(), null, null,
                buildPaxDetails(request));

        TraveloproHotelBookResponse response = restClient.post()
                .uri("/api/hotel-api-v6/hotel_book")
                .body(bookRequest)
                .retrieve()
                .body(TraveloproHotelBookResponse.class);

        if (response == null || !"CONFIRMED".equalsIgnoreCase(response.status())) {
            String reason = response == null ? "no response"
                    : response.error() != null ? response.error()
                    : response.Errors() != null ? response.Errors().ErrorMessage()
                    : "status " + response.status();
            throw new ProviderException("Travelopro hotel booking failed: " + reason);
        }

        String ref = response.referenceNum() != null ? response.referenceNum() : response.supplierConfirmationNum();
        return new ProviderBookingConfirmation(getType(), ref, LocalDateTime.now().plusHours(24), true);
    }

    /** Puts every guest in room 1, split into adult/child name arrays by passenger type. */
    private List<TraveloproHotelBookRequest.PaxDetail> buildPaxDetails(HotelBookingRequest request) {
        List<String> adultTitles = new ArrayList<>();
        List<String> adultFirst = new ArrayList<>();
        List<String> adultLast = new ArrayList<>();
        List<String> childTitles = new ArrayList<>();
        List<String> childFirst = new ArrayList<>();
        List<String> childLast = new ArrayList<>();

        for (PassengerInfo guest : request.guests()) {
            String[] nameParts = splitName(guest.fullName());
            if (guest.type() == com.guentours.provider.PassengerType.ADULT) {
                adultTitles.add("Mr");
                adultFirst.add(nameParts[0]);
                adultLast.add(nameParts[1]);
            } else {
                childTitles.add("Mstr");
                childFirst.add(nameParts[0]);
                childLast.add(nameParts[1]);
            }
        }

        TraveloproHotelBookRequest.PaxNames adults = adultFirst.isEmpty() ? null
                : new TraveloproHotelBookRequest.PaxNames(adultTitles, adultFirst, adultLast);
        TraveloproHotelBookRequest.PaxNames children = childFirst.isEmpty() ? null
                : new TraveloproHotelBookRequest.PaxNames(childTitles, childFirst, childLast);
        return List.of(new TraveloproHotelBookRequest.PaxDetail(1, adults, children));
    }

    /** Collapses the vendor's {@code |t|}-delimited cancellation-policy string into readable lines. */
    private String normalizeCancellationPolicy(String policy) {
        if (policy == null || policy.isBlank()) {
            return null;
        }
        return policy.replace("|t|", "\n").trim();
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
                .uri("/api/aeroVE5/booking")
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
