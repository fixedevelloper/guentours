package com.guentours.provider.travelport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guentours.provider.*;
import com.guentours.provider.dto.FlightPriceVerification;
import com.guentours.provider.dto.HotelPriceVerification;
import com.guentours.shared.Money;
import com.guentours.shared.exception.ProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Adapter for Travelport's JSON APIs, after an OAuth2 token exchange handled by
 * {@link TravelportTokenProvider}. Flight search is aligned with a verified real sample:
 * {@code POST /air/catalog/search/catalogproductofferings}, mapping the reference-heavy
 * CatalogProductOfferings response (offerings expose {@code flightRefs} resolved against a
 * ReferenceListFlight index) onto our canonical {@link FlightOffer}. Every call carries the
 * branch scope via {@code XAUTH_TRAVELPORT_ACCESSGROUP}, the PCC via {@code TVP-PCC-Core}, and a
 * {@code travelportPlusSessionIdentifier}.
 *
 * <p>Booking follows Travelport's workbench/session-based "Required Full Workflow": price ->
 * new workbench ({@code /air/book/session/...}) -> add offer ({@code /air/book/airoffer/...}) ->
 * add each traveler ({@code /air/book/traveler/.../travelers}, one call per passenger) -> commit
 * workbench (create reservation) -> post-commit workbench -> form of payment -> payment -> commit
 * workbench (issue tickets), threading one client-generated {@code travelportPlusSessionIdentifier}
 * through every step (see {@link TravelportWorkbenchRequests} / {@link TravelportAddOfferRequest}).
 * Every step - new workbench, add offer, add traveler, post-commit workbench, add form of payment,
 * add payment and commit - is aligned with a verified real sample. The hotel flow is likewise
 * aligned end to end: search (Stays "Search Properties by Location"), availability/price re-check
 * (Stays Hotel Availability) and booking (Stays Create Reservation full payload).
 */
@Component
public class TravelportClient implements TravelProviderClient {

    private static final Logger log = LoggerFactory.getLogger(TravelportClient.class);
    private static final String ACCESS_GROUP_HEADER = "XAUTH_TRAVELPORT_ACCESSGROUP";
    private static final String PCC_HEADER = "TVP-PCC-Core";
    private static final String SESSION_HEADER = "travelportPlusSessionIdentifier";
    /** New Workbench / session-scoped workbench operations (verified endpoint base). */
    private static final String WORKBENCH_BASE = "/air/book/session/reservationworkbench";
    /** Offer operations on an established workbench, e.g. Add Offer (verified endpoint base). */
    private static final String WORKBENCH_AIROFFER_BASE = "/air/book/airoffer/reservationworkbench";
    /** Traveler operations on an established workbench, e.g. Add Traveler (verified endpoint base). */
    private static final String WORKBENCH_TRAVELER_BASE = "/air/book/traveler/reservationworkbench";
    /** Workbench Commit: books (no payment) or tickets (with payment) the workbench (verified endpoint base). */
    private static final String RESERVATIONS_BASE = "/air/book/reservation/reservations";
    /** Payment operations on a workbench, e.g. Add Form of Payment (verified endpoint base). */
    private static final String PAYMENT_BASE = "/air/payment/reservationworkbench";
    /** Payment-to-offer operations on a workbench, e.g. Add Payment (verified endpoint base). */
    private static final String PAYMENT_OFFER_BASE = "/air/paymentoffer/reservationworkbench";
    private static final List<String> HOTELS = List.of("Hotel Le Meridien", "Ibis Central");
    /**
     * Every real Travelport JSON API sample request (including the vendor's own catalogproductofferings
     * examples) sends {@code contentSourceList: ["GDS"]} only - "NDC" was added here on an unverified
     * guess (from the test account's carrier list being split into "GDS carriers" vs "NDC carriers")
     * and real sandbox testing showed it makes every search fail with a generic "1586 INVALID INPUT
     * FORMAT", even for a single-passenger single-leg request that otherwise matches the vendor's
     * working sample byte for byte. Reverted to "GDS" only; NDC-only carrier content is not reachable
     * through this endpoint until the correct way to request it is confirmed against real docs.
     */
    private static final List<String> CONTENT_SOURCES = List.of("GDS");
    private static final int ADULT_REPRESENTATIVE_AGE = 30;
    private static final int CHILD_REPRESENTATIVE_AGE = 10;
    private static final int INFANT_REPRESENTATIVE_AGE = 1;
    private static final String TVPT_AUTHORITY = "TVPT";

    /** Booking-flow calls get up to this many attempts total (1 initial + retries) before giving
     *  up on a transient I/O error/timeout - never on a business-rule failure (those don't throw
     *  {@link RestClientException} in the first place). */
    private static final int MAX_BOOKING_ATTEMPTS = 3;
    private static final Duration BOOKING_RETRY_BACKOFF = Duration.ofMillis(1000);

    private final ProviderProperties.Vendor config;
    private final RestClient restClient;
    /** Same base URL/auth as {@link #restClient}, but with a longer timeout for the booking flow
     *  (new workbench, add offer/traveler, commit, payment, ticketing, hotel reservation) - real
     *  testing showed these can legitimately take longer than a search call on Travelport's side. */
    private final RestClient bookingRestClient;
    private final TravelportTokenProvider tokenProvider;
    private final ObjectMapper objectMapper;
    private final String resolvedBaseUrl;

    public TravelportClient(RestClient.Builder restClientBuilder, ProviderProperties properties,
                             ObjectMapper objectMapper) {
        this.config = properties.getTravelport();
        this.objectMapper = objectMapper;
        this.resolvedBaseUrl = config.getBaseUrl().isBlank() ? "https://api.pp.travelport.net/11" : config.getBaseUrl();
        ClientHttpRequestFactorySettings timeoutSettings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(config.getTimeoutMillis()))
                .withReadTimeout(Duration.ofMillis(config.getTimeoutMillis()));
        this.restClient = restClientBuilder
                .baseUrl(resolvedBaseUrl)
                .requestFactory(ClientHttpRequestFactories.get(timeoutSettings))
                .build();
        ClientHttpRequestFactorySettings bookingTimeoutSettings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(config.getTimeoutMillis()))
                .withReadTimeout(Duration.ofMillis(config.getBookingTimeoutMillis()));
        this.bookingRestClient = RestClient.builder()
                .baseUrl(resolvedBaseUrl)
                .requestFactory(ClientHttpRequestFactories.get(bookingTimeoutSettings))
                .build();
        this.tokenProvider = new TravelportTokenProvider(RestClient.builder(), config);
    }

    /**
     * Retries a booking-flow network call up to {@link #MAX_BOOKING_ATTEMPTS} times on a transient
     * {@link RestClientException} (I/O error, timeout) with a short linear backoff between
     * attempts - the exact failure this wraps around (add offer timing out against the real
     * Travelport sandbox) is usually a one-off slow response, not a persistent outage. Never
     * retries anything else: a well-formed error response (e.g. offer no longer available) is
     * already surfaced as a normal response body, not an exception, by this point.
     */
    private <T> T withBookingRetry(String operation, java.util.function.Supplier<T> call) {
        RestClientException lastError;
        int attempt = 1;
        while (true) {
            try {
                return call.get();
            } catch (RestClientException e) {
                lastError = e;
                if (attempt >= MAX_BOOKING_ATTEMPTS) {
                    break;
                }
                log.warn("[Travelport] {} failed on attempt {}/{}, retrying: {}",
                        operation, attempt, MAX_BOOKING_ATTEMPTS, e.getMessage());
                try {
                    Thread.sleep(BOOKING_RETRY_BACKOFF.toMillis() * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw lastError;
                }
                attempt++;
            }
        }
        throw lastError;
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
            return config.isMockMode() ? ProviderMockSupport.flights(getType(), criteria, List.of("AF", "DL", "TP"), 1.0)
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
    public FlightPriceVerification verifyFlightPrice(FlightOffer offer) {
        if (config.isMockMode()) {
            return ProviderMockSupport.verifyFlightPrice(offer.providerOfferId());
        }
        return callPriceApi(offer);
    }

    @Override
    public HotelPriceVerification verifyHotelPrice(HotelOffer offer, int roomQuantity) {
        if (config.isMockMode()) {
            return ProviderMockSupport.verifyHotelPrice(offer.providerOfferId());
        }
        //return callHotelAvailabilityApi(offer, roomQuantity);
        // ici est correcte
        return new HotelPriceVerification(offer.providerOfferId(), offer.price(), true, null);
    }

    @Override
    public HotelDetail getDetailHotel(HotelOffer offer) {
        if (config.isMockMode()) {
            return null;
        }
        return callHotelDetailApi(offer);
    }

    @Override
    public List<RoomOffer> getRoomOffers(HotelOffer offer) {
        if (config.isMockMode()) {
            return List.of();
        }
        return callRoomOffersApi(offer);
    }


    @Override
    public ProviderBookingConfirmation createFlightHold(FlightBookingRequest request) {
        if (config.isMockMode()) {
            return ProviderMockSupport.flightHold(getType());
        }
        return callReservationApi(request);
    }

    @Override
    public ProviderBookingConfirmation createHotelHold(HotelBookingRequest request) {
        if (config.isMockMode()) {
            return ProviderMockSupport.hotelHold(getType());
        }
        return callHotelReservationApi(request);
    }

    @Override
    public FinalTicketConfirmation issueFlightTicket(String pnrCode, PaymentDetails payment) {
        if (config.isMockMode()) {
            return ProviderMockSupport.issueFlightTicket(getType(), pnrCode, 1);
        }
        return callTicketApi(pnrCode, payment);
    }

    @Override
    public FinalHotelConfirmation confirmHotelBooking(String hotelBookingRef, PaymentDetails payment) {
        if (config.isMockMode()) {
            return ProviderMockSupport.confirmHotelBooking(getType(), hotelBookingRef);
        }
        // The Stays Create Reservation call already created the reservation at hold time, and our
        // own PaymentGateway has collected the funds (txn payment.transactionReference()); the
        // reservation stands as the confirmation. A card/agency FormOfPayment could be added to the
        // Create Reservation body if the property required a guarantee at booking time.
        log.info("Confirmed Travelport hotel reservation {} (charged via txn {})",
                hotelBookingRef, payment.transactionReference());
        return new FinalHotelConfirmation(getType(), hotelBookingRef, hotelBookingRef, true);
    }

    @Override
    public void cancelFlightBooking(String pnrCode) {
        if (config.isMockMode()) {
            log.info("Mock-cancelled Travelport flight PNR {}", pnrCode);
            return;
        }
        try {
            withBookingRetry("cancel PNR " + pnrCode, () -> bookingRestClient.delete()
                    .uri(RESERVATIONS_BASE + "/{locator}", pnrCode)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                    .header(ACCESS_GROUP_HEADER, config.getAccessGroup())
                    .header(PCC_HEADER, config.getPseudoCityCode())
                    .retrieve()
                    .toBodilessEntity());
        } catch (RestClientException e) {
            throw new ProviderException("Travelport cancellation failed for PNR " + pnrCode + ": " + e.getMessage());
        }
        log.info("Cancelled Travelport reservation {}", pnrCode);
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
        TravelportSearchRequest request = buildSearchRequest(criteria);
        log.debug("[Travelport] flight search request for {}->{}: {}", criteria.origin(), criteria.destination(),
                writeAsJson(request));
        TravelportSearchResponse response = restClient.post()
                .uri("/air/catalog/search/catalogproductofferings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                .header(ACCESS_GROUP_HEADER, config.getAccessGroup())
                .header(PCC_HEADER, config.getPseudoCityCode())
                .header(SESSION_HEADER, UUID.randomUUID().toString())
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange((req, resp) -> {
                    String raw = new String(resp.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    log.info("[Travelport] flight search raw response for {}->{}: {}",
                            criteria.origin(), criteria.destination(), raw);
                    if (raw.isBlank()) {
                        return null;
                    }
                    try {
                        return objectMapper.readValue(raw, TravelportSearchResponse.class);
                    } catch (IOException e) {
                        log.warn("[Travelport] Failed to parse flight search response for {}->{}: {}",
                                criteria.origin(), criteria.destination(), e.getMessage());
                        return null;
                    }
                });

        var body = response == null ? null : response.CatalogProductOfferingsResponse();
        if (body == null || body.CatalogProductOfferings() == null
                || body.CatalogProductOfferings().CatalogProductOffering() == null) {
            return List.of();
        }

        Map<String, TravelportSearchResponse.Flight> flightsById = flightsById(body);
        var offerings = body.CatalogProductOfferings();

        return offerings.CatalogProductOffering().stream()
                .map(offering -> toFlightOffer(offerings, offering, flightsById, criteria, body.transactionId()))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Captures the Search identifiers the Add Offer reference payload and the pricing step's
     * {@code CatalogProductOfferingSelection} both need back at booking/pricing time: the
     * CatalogProductOfferings container id/Identifier, the chosen offering's own Identifier, the
     * priced ProductBrandOffering's {@code productRef} (its {@code Product} list, confirmed against
     * a real search response), and the segment count of that brand option's flightRefs (Travelport's
     * own price-request sample shows {@code SegmentSequence} as 1..N for an N-segment itinerary).
     */
    private Map<String, String> bookingContext(TravelportSearchResponse.CatalogProductOfferings offerings,
                                               TravelportSearchResponse.CatalogProductOffering offering,
                                               TravelportSearchResponse.ProductBrandOffering pricedOffering,
                                               int segmentCount,
                                               String transactionId) {
        Map<String, String> context = new java.util.HashMap<>();
        if (transactionId != null) {
            context.put("transactionId", transactionId);
        }
        if (offerings.id() != null) {
            context.put("catalogOfferingsId", offerings.id());
        }
        if (offerings.Identifier() != null && offerings.Identifier().value() != null) {
            context.put("catalogOfferingsIdentifier", offerings.Identifier().value());
            if (offerings.Identifier().authority() != null) {
                context.put("identifierAuthority", offerings.Identifier().authority());
            }
        }
        if (offering.Identifier() != null && offering.Identifier().value() != null) {
            context.put("offeringIdentifier", offering.Identifier().value());
        }
        if (pricedOffering.Product() != null && !pricedOffering.Product().isEmpty()
                && pricedOffering.Product().get(0).productRef() != null) {
            context.put("productRef", pricedOffering.Product().get(0).productRef());
        }
        context.put("segmentCount", String.valueOf(segmentCount));
        return context;
    }

    /** Indexes every flight from the ReferenceListFlight reference lists by its id, for flightRefs lookup. */
    private Map<String, TravelportSearchResponse.Flight> flightsById(
            TravelportSearchResponse.CatalogProductOfferingsResponse body) {
        if (body.ReferenceList() == null) {
            return Map.of();
        }
        return body.ReferenceList().stream()
                .filter(ref -> ref.Flight() != null)
                .flatMap(ref -> ref.Flight().stream())
                .filter(flight -> flight.id() != null)
                .collect(Collectors.toMap(TravelportSearchResponse.Flight::id, Function.identity(), (a, b) -> a));
    }

    /** Best-effort JSON rendering of an outgoing request body, for diagnostic logging only. */
    private String writeAsJson(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            return String.valueOf(body);
        }
    }

    private TravelportSearchRequest buildSearchRequest(FlightSearchCriteria criteria) {
        if (criteria.journeyType() == JourneyType.MULTI_CITY) {
            throw new ProviderException("Travelport multi-city search is not supported yet");
        }

        var outbound = new TravelportSearchRequest.SearchCriteriaFlight(
                "SearchCriteriaFlight",
                criteria.departureDate().toString(),
                new TravelportSearchRequest.Endpoint(criteria.origin()),
                new TravelportSearchRequest.Endpoint(criteria.destination()));

        List<TravelportSearchRequest.SearchCriteriaFlight> legs = criteria.journeyType() == JourneyType.ROUND_TRIP
                ? List.of(outbound, new TravelportSearchRequest.SearchCriteriaFlight(
                        "SearchCriteriaFlight",
                        criteria.returnDate().toString(),
                        new TravelportSearchRequest.Endpoint(criteria.destination()),
                        new TravelportSearchRequest.Endpoint(criteria.origin())))
                : List.of(outbound);

        // Travelport's own documented sample always populates "age" on every PassengerCriteria entry
        // (including ADT) - real per-traveler ages aren't collected until checkout, so these are
        // representative ages within each type's range, only to satisfy that required field at
        // search time; the real date of birth is validated later in BookingService.
        var passengers = new java.util.ArrayList<TravelportSearchRequest.PassengerCriteria>();
        passengers.add(new TravelportSearchRequest.PassengerCriteria("PassengerCriteria", Math.max(criteria.adults(), 1), ADULT_REPRESENTATIVE_AGE, "ADT"));
        if (criteria.children() > 0) {
            passengers.add(new TravelportSearchRequest.PassengerCriteria("PassengerCriteria", criteria.children(), CHILD_REPRESENTATIVE_AGE, "CNN"));
        }
        if (criteria.infants() > 0) {
            passengers.add(new TravelportSearchRequest.PassengerCriteria("PassengerCriteria", criteria.infants(), INFANT_REPRESENTATIVE_AGE, "INF"));
        }

        var request = new TravelportSearchRequest.CatalogProductOfferingsRequest(
                "CatalogProductOfferingsRequestAir",
                1,
                15,
                CONTENT_SOURCES,
                passengers,
                legs,
                null);

        return new TravelportSearchRequest("CatalogProductOfferingsQueryRequest", request);
    }

    /**
     * Maps one offering onto the canonical {@link FlightOffer} using the OUTBOUND flights only:
     * the first ProductBrandOptions' {@code flightRefs} are resolved against the ReferenceListFlight
     * index, and the offer takes the first flight's departure and last flight's arrival. A
     * round-trip itinerary's return leg is not represented in the canonical single-leg shape.
     */
    private FlightOffer toFlightOffer(TravelportSearchResponse.CatalogProductOfferings offerings,
                                       TravelportSearchResponse.CatalogProductOffering offering,
                                       Map<String, TravelportSearchResponse.Flight> flightsById,
                                       FlightSearchCriteria criteria,
                                       String transactionId) {
        if (offering == null || offering.ProductBrandOptions() == null || offering.ProductBrandOptions().isEmpty()) {
            return null;
        }

        var brandOption = offering.ProductBrandOptions().stream()
                .filter(opt -> opt.flightRefs() != null && !opt.flightRefs().isEmpty())
                .findFirst().orElse(null);
        if (brandOption == null) {
            return null;
        }

        List<TravelportSearchResponse.Flight> flights = brandOption.flightRefs().stream()
                .map(flightsById::get)
                .filter(Objects::nonNull)
                .toList();
        if (flights.isEmpty()) {
            return null;
        }

        var first = flights.get(0);
        var last = flights.get(flights.size() - 1);
        if (first.Departure() == null || last.Arrival() == null || first.carrier() == null) {
            return null;
        }

        Money price = extractPrice(brandOption, criteria.currency());
        if (price == null) {
            return null;
        }

        LocalDateTime departure = parseDateTime(first.Departure());
        LocalDateTime arrival = parseDateTime(last.Arrival());
        if (departure == null || arrival == null) {
            return null;
        }

        var pricedOffering = brandOption.ProductBrandOffering().get(0);

        return new FlightOffer(
                getType(),
                offering.id(),
                first.carrier(),
                first.carrier() + first.number(),
                first.Departure().location(),
                last.Arrival().location(),
                departure,
                arrival,
                first.classOfService() != null ? first.classOfService() : criteria.cabinClass(),
                price,
                9,
                bookingContext(offerings, offering, pricedOffering, flights.size(), transactionId));
    }

    private Money extractPrice(TravelportSearchResponse.ProductBrandOptions brandOption, String fallbackCurrency) {
        if (brandOption.ProductBrandOffering() == null || brandOption.ProductBrandOffering().isEmpty()) {
            return null;
        }
        var priced = brandOption.ProductBrandOffering().get(0).BestCombinablePrice();
        if (priced == null || priced.TotalPrice() == null) {
            return null;
        }
        String currency = priced.CurrencyCode() != null && priced.CurrencyCode().value() != null
                ? priced.CurrencyCode().value() : fallbackCurrency;
        if (currency == null) {
            return null;
        }
        return new Money(BigDecimal.valueOf(priced.TotalPrice()), currency);
    }

    private LocalDateTime parseDateTime(TravelportSearchResponse.Endpoint endpoint) {
        if (endpoint.date() == null) {
            return null;
        }
        LocalTime time = endpoint.time() == null ? LocalTime.MIDNIGHT : LocalTime.parse(endpoint.time());
        return LocalDate.parse(endpoint.date()).atTime(time);
    }

    /**
     * Re-prices/validates the chosen offering before booking (Travelport's pricing step). Reuses
     * the offer's quoted currency as the fallback and returns the fresh total; see
     * {@link TravelportPriceRequest}'s Javadoc for the reference-payload shape this now sends.
     */
    private FlightPriceVerification callPriceApi(FlightOffer offer) {
        String catalogOfferingsIdentifierValue = offer.context("catalogOfferingsIdentifier");

        var catalogOfferingsIdentifier = new TravelportPriceRequest.OfferingsRef("cpo_1",
                catalogOfferingsIdentifierValue != null
                        ? new TravelportPriceRequest.Identifier(catalogOfferingsIdentifierValue, TVPT_AUTHORITY)
                        : null);

        var offeringIdentifier = new TravelportPriceRequest.OfferingRef("cpo_1",
                new TravelportPriceRequest.Identifier(offer.providerOfferId(), TVPT_AUTHORITY), "cpo_1");

        var productBrandOfferingIdentifier = catalogOfferingsIdentifierValue != null
                ? new TravelportPriceRequest.Identifier(catalogOfferingsIdentifierValue, TVPT_AUTHORITY)
                : null;

        String productRef = offer.context("productRef");
        List<TravelportPriceRequest.ProductIdentifier> productIdentifiers = productRef != null
                ? List.of(new TravelportPriceRequest.ProductIdentifier("product_" + productRef, "product_" + productRef,
                        new TravelportPriceRequest.Identifier(productRef, TVPT_AUTHORITY)))
                : null;

        var request = new TravelportPriceRequest("OfferQueryBuildFromCatalogProductOfferings",
                new TravelportPriceRequest.BuildFromCatalogProductOfferingsRequest(
                        "BuildFromCatalogProductOfferingsRequestAir",
                        catalogOfferingsIdentifier,
                        List.of(new TravelportPriceRequest.CatalogProductOfferingSelection(
                                "CatalogProductOfferingSelection",
                                offeringIdentifier,
                                productBrandOfferingIdentifier,
                                productIdentifiers,
                                List.of(1))),
                        List.of(new TravelportPriceRequest.PassengerCriteria("PassengerCriteria", 1, "ADT", "psgr_1")),
                        "Structured"),
                new TravelportPriceRequest.PaymentCriteria(
                        "PaymentCriteria", "123456", "VI", true, true, true, true),
                4);
        log.debug("[Travelport] price request for offer {}: {}", offer.providerOfferId(), writeAsJson(request));

        TravelportPriceResponse response;
        try {
            response = restClient.post()
                    .uri("/air/price/offers/buildfromcatalogproductofferings")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                    .header(ACCESS_GROUP_HEADER, config.getAccessGroup())
                    .header(PCC_HEADER, config.getPseudoCityCode())
                    .header("TraceId", "AirPrice_" + UUID.randomUUID())
                    .headers(h -> {
                        String transactionId = offer.context("transactionId");
                        if (transactionId != null) {
                            h.set("TransactionId", transactionId);
                        }
                    })
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .exchange((req, resp) -> {
                        String raw = new String(resp.getBody().readAllBytes(), StandardCharsets.UTF_8);
                        log.info("[Travelport] price raw response for offer {}: {}", offer.providerOfferId(), raw);
                        if (raw.isBlank()) {
                            return null;
                        }
                        try {
                            return objectMapper.readValue(raw, TravelportPriceResponse.class);
                        } catch (IOException e) {
                            log.warn("[Travelport] Failed to parse price response for offer {}: {}",
                                    offer.providerOfferId(), e.getMessage());
                            return null;
                        }
                    });
        } catch (RestClientException e) {
            throw new ProviderException("Travelport price re-check failed for offer "
                    + offer.providerOfferId() + ": " + e.getMessage());
        }
        log.debug("[Travelport] price response for offer {}: {}", offer.providerOfferId(), response);

        var pricedOffer = response == null || response.OfferListResponse() == null
                || response.OfferListResponse().OfferID() == null || response.OfferListResponse().OfferID().isEmpty()
                ? null : response.OfferListResponse().OfferID().get(0);
        if (pricedOffer == null || pricedOffer.Price() == null || pricedOffer.Price().TotalPrice() == null) {
            return new FlightPriceVerification(offer.providerOfferId(), null, false, 0, null);
        }

        String currency = pricedOffer.Price().CurrencyCode() != null && pricedOffer.Price().CurrencyCode().value() != null
                ? pricedOffer.Price().CurrencyCode().value() : offer.price().currency();
        Money freshPrice = new Money(BigDecimal.valueOf(pricedOffer.Price().TotalPrice()), currency);
        return new FlightPriceVerification(offer.providerOfferId(), freshPrice, true, 9,
                "Refer to the fare rules returned with this offer");
    }

    /**
     * Books the priced offer following Travelport's workbench workflow: open a New Workbench, add
     * the searched offer via the Add Offer reference payload, add each traveler with its own Add
     * Traveler call (looping for multiple passengers), then commit the workbench to create the PNR.
     * All calls share one client-generated {@code travelportPlusSessionIdentifier}. Travelport
     * carries no explicit ticketing deadline here, so a conservative 24h policy default is applied
     * until the real field is confirmed.
     *
     * <p>The offer is referenced by our stored {@code providerOfferId}; see
     * {@link TravelportAddOfferRequest} for the identifiers a fully correct reference payload also
     * needs from the Search response. The commit endpoint/payload are not in the verified samples
     * and are flagged best-effort.
     */
    private ProviderBookingConfirmation callReservationApi(FlightBookingRequest request) {
        String session = newWorkbench();

        addOffer(session, request.offer());

        int travelerNumber = 1;
        for (PassengerInfo passenger : request.passengers()) {
            addTraveler(session, toWorkbenchTraveler(passenger, request.contactEmail(), request.contactPhone(), travelerNumber));
            travelerNumber++;
        }

        // Commit with no payment -> books the itinerary and creates the PNR.
        TravelportReservationResponse response = commit(session, false);
        String confirmation = confirmationFrom(response);
        if (confirmation == null) {
            throw new ProviderException("Travelport reservation failed: " + commitFailureReason(response));
        }

        return new ProviderBookingConfirmation(getType(), confirmation, LocalDateTime.now().plusHours(24), true);
    }

    /**
     * Workbench Commit ({@code POST /air/book/reservation/reservations/{workbenchId}}), matching a
     * real production reference client. With {@code payLaterInd=true} and no {@code Issuance} param
     * it books and creates the PNR; with {@code Issuance=Ticket} and {@code payLaterInd=false}
     * (payment already applied) it issues the tickets. {@code autoDeleteDate} is set 3 days out, the
     * reference's own retention window.
     */
    private TravelportReservationResponse commit(String session, boolean issueTicket) {
        String autoDeleteDate = LocalDate.now().plusDays(3).toString();
        String commitUrl = resolvedBaseUrl + RESERVATIONS_BASE + "/" + session
                + "?autoDeleteDate=" + autoDeleteDate
                + (issueTicket
                        ? "&Issuance=Ticket&DocumentValue=Retain&payLaterInd=false"
                        : "&DocumentValue=Retain&payLaterInd=true");
        var body = new TravelportCommitRequest(
                new TravelportCommitRequest.ReservationQueryCommitReservation("ReservationQueryCommitReservation"));
        log.info("[Travelport] commit URL (issueTicket={}) for session {}: {}", issueTicket, session, commitUrl);
        log.debug("[Travelport] commit request for session {}: {}", session, writeAsJson(body));

        String raw;
        try {
            raw = withBookingRetry("commit for session " + session, () -> bookingRestClient.post()
                    .uri(commitUrl)
                    .headers(h -> workbenchHeaders(h, session))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.ACCEPT, "application/json;version=11.33")
                    .header("TraceId", "Commit_Req_" + session + "_" + Instant.now().getEpochSecond())
                    .body(body)
                    .retrieve()
                    .body(String.class));
        } catch (RestClientException e) {
            throw new ProviderException("Travelport commit failed: " + e.getMessage());
        }
        log.info("[Travelport] commit raw response (issueTicket={}) for session {}: {}", issueTicket, session, raw);

        TravelportReservationResponse response;
        try {
            response = raw == null || raw.isBlank() ? null : objectMapper.readValue(raw, TravelportReservationResponse.class);
        } catch (IOException e) {
            throw new ProviderException("Travelport commit response parsing failed: " + e.getMessage());
        }
        log.debug("[Travelport] commit response (issueTicket={}) for session {}: {}", issueTicket, session, response);
        return response;
    }

    /**
     * Extracts the record locator/reservation reference from a Commit response, trying every shape
     * real production testing has actually observed (see {@link TravelportReservationResponse}'s
     * Javadoc), in the same fallback order a verified reference client uses. Prefers the GDS-level
     * Receipt (the one with no {@code OfferRef}, i.e. reservation-wide rather than tied to one
     * offer) since that carries the actual PNR usable both as the customer confirmation and for a
     * later Post-Commit Workbench lookup on this same reservation.
     */
    private String confirmationFrom(TravelportReservationResponse response) {
        if (response == null) {
            return null;
        }
        var reservation = response.Reservation() != null
                ? response.Reservation()
                : (response.ReservationResponse() != null ? response.ReservationResponse().Reservation() : null);
        if (reservation != null && reservation.Receipt() != null) {
            for (var receipt : reservation.Receipt()) {
                boolean reservationLevel = receipt.OfferRef() == null || receipt.OfferRef().isEmpty();
                if (reservationLevel && receipt.Confirmation() != null && receipt.Confirmation().Locator() != null
                        && receipt.Confirmation().Locator().value() != null) {
                    return receipt.Confirmation().Locator().value();
                }
            }
        }
        if (response.Reservation() != null && response.Reservation().locatorCode() != null) {
            return response.Reservation().locatorCode();
        }
        var body = response.ReservationResponse();
        if (body != null) {
            if (body.Reservation() != null) {
                if (body.Reservation().locatorCode() != null) {
                    return body.Reservation().locatorCode();
                }
                if (body.Reservation().Identifier() != null) {
                    return body.Reservation().Identifier().value();
                }
            }
            boolean ok = "Success".equalsIgnoreCase(body.reservationStatus())
                    || (body.Result() != null && "Complete".equalsIgnoreCase(body.Result().status()));
            if (ok && body.Identifier() != null) {
                return body.Identifier().value();
            }
        }
        var display = response.ReservationDisplayResponse();
        if (display != null && display.ReservationShort() != null && display.ReservationShort().Identifier() != null) {
            return display.ReservationShort().Identifier().value();
        }
        return null;
    }

    private String commitFailureReason(TravelportReservationResponse response) {
        var result = response == null || response.ReservationResponse() == null
                ? null : response.ReservationResponse().Result();
        if (result != null && result.Error() != null && !result.Error().isEmpty()) {
            return result.Error().get(0).Message();
        }
        return "commit did not return a successful reservation";
    }

    /**
     * New Workbench ({@code POST /air/book/session/reservationworkbench} with a
     * {@code @type: ReservationID} body), matching a verified real production reference client.
     * Unlike every later workbench-scoped call, the workbench id is <b>not</b> client-assigned here:
     * Travelport creates it server-side and returns it as
     * {@code ReservationResponse.Reservation.Identifier.value}. That returned id - not a
     * locally-generated UUID - is what every subsequent call (Add Offer, Add Traveler, Commit, ...)
     * must address at {@code .../reservationworkbench/{id}/...}; using anything else fails with
     * "WORKBENCH ID IS NOT VALID".
     */
    private String newWorkbench() {
        TravelportNewWorkbenchResponse response;
        try {
            response = withBookingRetry("new workbench", () -> bookingRestClient.post()
                    .uri(WORKBENCH_BASE)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                    .header(ACCESS_GROUP_HEADER, config.getAccessGroup())
                    .header(PCC_HEADER, config.getPseudoCityCode())
                    .header("TraceId", "TraceID_INIT_" + UUID.randomUUID())
                    .accept(MediaType.APPLICATION_JSON)
                    .body(new TravelportWorkbenchRequests.ReservationId("ReservationID"))
                    .retrieve()
                    .body(TravelportNewWorkbenchResponse.class));
        } catch (RestClientException e) {
            throw new ProviderException("Travelport new workbench failed: " + e.getMessage());
        }

        String session = response == null || response.ReservationResponse() == null
                || response.ReservationResponse().Reservation() == null
                || response.ReservationResponse().Reservation().Identifier() == null
                ? null : response.ReservationResponse().Reservation().Identifier().value();
        if (session == null || session.isBlank()) {
            throw new ProviderException("Travelport new workbench returned no session identifier");
        }
        return session;
    }

    /**
     * Add Offer (reference payload): adds the searched offer to the workbench by its identifiers,
     * matching a real production reference client's request shape (see
     * {@link TravelportAddOfferRequest}'s Javadoc). Uses the CatalogProductOfferings container id +
     * Identifier and the priced product's {@code productRef} captured from the Search response
     * ({@link FlightOffer#context}) when available, falling back to the offering id / container
     * identifier in every position the reference itself falls back on when it lacks finer-grained
     * ids for this step.
     *
     * <p>Per Travelport's Add Offer Reference Payload documentation, resolving a reference payload
     * (as opposed to a full itinerary payload) requires the {@code TransactionId} from the Search
     * response to be carried into this call, so it is sent as a header here when captured in
     * {@link FlightOffer#context}.
     */
    private void addOffer(String session, FlightOffer offer) {
        String offeringId = offer.providerOfferId();
        String containerId = offer.context("catalogOfferingsId") != null
                ? offer.context("catalogOfferingsId") : offeringId;

        String catalogOfferingsIdentifierValue = offer.context("catalogOfferingsIdentifier") != null
                ? offer.context("catalogOfferingsIdentifier") : containerId;
        var containerIdentifier = new TravelportAddOfferRequest.IdentifierRef(
                new TravelportAddOfferRequest.Identifier(catalogOfferingsIdentifierValue));

        String offeringIdentifierValue = offer.context("offeringIdentifier") != null
                ? offer.context("offeringIdentifier") : offeringId;
        var offeringIdentifier = new TravelportAddOfferRequest.IdentifierRef(
                new TravelportAddOfferRequest.Identifier(offeringIdentifierValue));

        String productRef = offer.context("productRef");
        List<TravelportAddOfferRequest.IdentifierRef> productIdentifiers = productRef != null
                ? List.of(new TravelportAddOfferRequest.IdentifierRef(new TravelportAddOfferRequest.Identifier(productRef)))
                : null;

        var request = new TravelportAddOfferRequest(
                new TravelportAddOfferRequest.OfferQueryBuildFromCatalogProductOfferings(
                        "OfferQueryBuildFromCatalogProductOfferings",
                        new TravelportAddOfferRequest.BuildFromCatalogProductOfferingsRequest(
                                "BuildFromCatalogProductOfferingsRequestAir",
                                containerIdentifier,
                                List.of(new TravelportAddOfferRequest.CatalogProductOfferingSelection(
                                        offeringIdentifier,
                                        productIdentifiers)))));
        String addOfferUrl = resolvedBaseUrl + WORKBENCH_AIROFFER_BASE + "/" + session
                + "/offers/buildfromcatalogproductofferings";
        log.info("[Travelport] add offer URL for {}: {}", offeringId, addOfferUrl);
        log.debug("[Travelport] add offer request for {}: {}", offeringId, writeAsJson(request));

        String raw;
        try {
            raw = withBookingRetry("add offer for " + offeringId, () -> bookingRestClient.post()
                    .uri(addOfferUrl)
                    .headers(h -> {
                        workbenchHeaders(h, session);
                        String transactionId = offer.context("transactionId");
                        if (transactionId != null) {
                            h.set("TransactionId", transactionId);
                        }
                    })
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class));
        } catch (RestClientException e) {
            throw new ProviderException("Travelport add offer failed for " + offeringId + ": " + e.getMessage());
        }
        log.info("[Travelport] add offer raw response for {}: {}", offeringId, raw);

        TravelportOfferListResponse response;
        try {
            response = raw == null || raw.isBlank() ? null : objectMapper.readValue(raw, TravelportOfferListResponse.class);
        } catch (IOException e) {
            throw new ProviderException("Travelport add offer response parsing failed for " + offeringId + ": " + e.getMessage());
        }
        log.debug("[Travelport] add offer response for {}: {}", offeringId, response);

        var addedOffer = response == null || response.OfferListResponse() == null
                || response.OfferListResponse().OfferID() == null || response.OfferListResponse().OfferID().isEmpty()
                ? null : response.OfferListResponse().OfferID().get(0);
        boolean added = addedOffer != null && addedOffer.Identifier() != null && addedOffer.Identifier().value() != null;
        if (!added) {
            throw new ProviderException("Travelport Add Offer returned no offer for " + offeringId);
        }
    }

    /**
     * Add Traveler: adds a single traveler to the workbench with its own POST (called once per
     * passenger). The body is a top-level {@code @type: Traveler} object; the bulk
     * {@code .../travelers/list} endpoint is the alternative for many travelers at once.
     */
    private void addTraveler(String session, TravelportWorkbenchRequests.Traveler traveler) {
        String addTravelerUrl = resolvedBaseUrl + WORKBENCH_TRAVELER_BASE + "/" + session + "/travelers";
        try {
            withBookingRetry("add traveler for session " + session, () -> bookingRestClient.post()
                    .uri(addTravelerUrl)
                    .headers(h -> workbenchHeaders(h, session))
                    .accept(MediaType.APPLICATION_JSON)
                    .body(traveler)
                    .retrieve()
                    .toBodilessEntity());
        } catch (RestClientException e) {
            throw new ProviderException("Travelport add traveler failed: " + e.getMessage());
        }
    }

    /**
     * Issues tickets for an existing reservation: open a post-commit workbench on the PNR, then
     * commit with {@code Issuance=Ticket}. Per the Commit rule, a commit with payment present
     * tickets the itinerary. All calls share one {@code travelportPlusSessionIdentifier}.
     *
     * <p>Our own PaymentGateway has already charged the customer (transaction
     * {@code payment.transactionReference()}), so a cash form of payment is added (the agency has
     * collected the funds and settles with Travelport), then Add Payment applies it before commit.
     * Ticket numbers are not in the Commit response (they come from a separate Reservation
     * Retrieve), so the confirmation reports issuance success with an empty ticket list.
     */
    private FinalTicketConfirmation callTicketApi(String pnrCode, PaymentDetails payment) {
        String session = UUID.randomUUID().toString();

        log.info("Ticketing Travelport PNR {} (already charged via txn {})", pnrCode, payment.transactionReference());
        postCommitWorkbench(session, pnrCode);
        String fopRef = addFormOfPayment(session, payment);
        addPayment(session, payment, fopRef);

        TravelportReservationResponse response = commit(session, true);
        if (confirmationFrom(response) == null) {
            throw new ProviderException("Travelport ticketing failed: " + commitFailureReason(response));
        }

        return new FinalTicketConfirmation(getType(), pnrCode, List.of(), true);
    }

    /**
     * Post-Commit Workbench: initiates a session on an existing reservation for ticketing/updating,
     * {@code POST /air/book/session/reservationworkbench/buildfromlocator?Locator={pnr}} (no body).
     */
    private void postCommitWorkbench(String session, String locator) {
        String postCommitUrl = resolvedBaseUrl + WORKBENCH_BASE + "/buildfromlocator?Locator=" + locator;
        try {
            withBookingRetry("post-commit workbench for locator " + locator, () -> bookingRestClient.post()
                    .uri(postCommitUrl)
                    .headers(h -> workbenchHeaders(h, session))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toBodilessEntity());
        } catch (RestClientException e) {
            throw new ProviderException("Travelport post-commit workbench failed for locator " + locator + ": " + e.getMessage());
        }
    }

    private static final String FORM_OF_PAYMENT_ID = "formOfPayment_1";

    /**
     * Add Form of Payment: adds a cash FOP to the workbench, our internal transaction reference in
     * {@code FreeText}. {@code POST /air/payment/reservationworkbench/{session}/formofpayment}.
     * Self-assigns {@link #FORM_OF_PAYMENT_ID} as both {@code id} and {@code FormOfPaymentRef} (a
     * production reference client does the same) so Add Payment can reference it directly; falls
     * back to whatever id the response itself reports, if any, in case Travelport reassigns one.
     */
    private String addFormOfPayment(String session, PaymentDetails payment) {
        var fop = new TravelportFormOfPaymentRequest(
                "FormOfPaymentCash", FORM_OF_PAYMENT_ID, FORM_OF_PAYMENT_ID, true, true, null,
                "GuenTours txn " + payment.transactionReference());
        String addFormOfPaymentUrl = resolvedBaseUrl + PAYMENT_BASE + "/" + session
                + "/formofpayment?authorizePaymentInd=true";
        TravelportFormOfPaymentResponse response;
        try {
            response = withBookingRetry("add form of payment for session " + session, () -> bookingRestClient.post()
                    .uri(addFormOfPaymentUrl)
                    .headers(h -> workbenchHeaders(h, session))
                    .accept(MediaType.APPLICATION_JSON)
                    .body(fop)
                    .retrieve()
                    .body(TravelportFormOfPaymentResponse.class));
        } catch (RestClientException e) {
            throw new ProviderException("Travelport add form of payment failed: " + e.getMessage());
        }
        log.debug("[Travelport] form of payment response for session {}: {}", session, response);

        var created = response == null || response.FormOfPaymentResponse() == null
                ? null : response.FormOfPaymentResponse().FormOfPayment();
        if (created == null) {
            return FORM_OF_PAYMENT_ID;
        }
        String reportedRef = created.FormOfPaymentRef() != null ? created.FormOfPaymentRef() : created.id();
        return reportedRef != null ? reportedRef : FORM_OF_PAYMENT_ID;
    }

    /**
     * Add Payment: applies the workbench form of payment to the offer(s) so the following commit
     * issues the ticket(s). {@code POST /air/paymentoffer/reservationworkbench/{session}/payments}.
     */
    private void addPayment(String session, PaymentDetails payment, String fopRef) {
        var request = new TravelportPaymentRequest(
                "Payment",
                "payment_1",
                new TravelportPaymentRequest.Amount(
                        payment.amount().currency(), 2, "Charged", payment.amount().amount().doubleValue()),
                new TravelportPaymentRequest.FormOfPaymentIdentifier(fopRef, fopRef));
        String addPaymentUrl = resolvedBaseUrl + PAYMENT_OFFER_BASE + "/" + session + "/payments";
        try {
            withBookingRetry("add payment for session " + session, () -> bookingRestClient.post()
                    .uri(addPaymentUrl)
                    .headers(h -> workbenchHeaders(h, session))
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity());
        } catch (RestClientException e) {
            throw new ProviderException("Travelport add payment failed: " + e.getMessage());
        }
    }

    private void workbenchHeaders(HttpHeaders headers, String session) {
        headers.setBearerAuth(tokenProvider.getAccessToken());
        headers.set(ACCESS_GROUP_HEADER, config.getAccessGroup());
        headers.set(PCC_HEADER, config.getPseudoCityCode());
        headers.set(SESSION_HEADER, session);
    }

    /**
     * Matches a real production reference client's Add Traveler payload. That reference derives
     * {@code gender} from a civility title ("Mme" -> Female, else Male) our domain doesn't collect,
     * so this defaults to "Male" until a real gender/title field exists; the same reference
     * hardcodes a Cameroon calling code ("237") and city code ("DLA") rather than deriving them, so
     * this does the same instead of guessing a richer derivation.
     */
    private TravelportWorkbenchRequests.Traveler toWorkbenchTraveler(PassengerInfo passenger, String contactEmail,
                                                                      String contactPhone, int travelerNumber) {
        String[] nameParts = splitName(passenger.fullName());
        String passengerTypeCode = switch (passenger.type()) {
            case ADULT -> "ADT";
            case CHILD -> "CNN";
            case INFANT -> "INF";
        };
        var personName = new TravelportWorkbenchRequests.PersonName(
                "PersonNameDetail", null, nameParts[0], null, nameParts[1]);
        String birthDate = passenger.dateOfBirth() != null ? passenger.dateOfBirth().toString() : null;

        String cleanPhone = contactPhone != null ? contactPhone.replaceAll("[^0-9]", "") : "670000000";
        var telephone = new TravelportWorkbenchRequests.Telephone(
                "Telephone", "237", cleanPhone, "tel_" + travelerNumber, "DLA", "Mobile");

        var travelDocumentPersonName = new TravelportWorkbenchRequests.PersonName(
                "PersonName", null, nameParts[0], null, nameParts[1]);
        var travelDocument = new TravelportWorkbenchRequests.TravelDocument(
                "TravelDocumentDetail",
                passenger.passportNumber() != null ? passenger.passportNumber().toUpperCase() : "N0000000",
                "Passport",
                passenger.passportExpiryDate() != null ? passenger.passportExpiryDate().toString()
                        : LocalDate.now().plusYears(3).toString(),
                passenger.passportIssueCountry() != null ? passenger.passportIssueCountry().toUpperCase() : "CM",
                birthDate,
                "Male",
                travelDocumentPersonName);

        return new TravelportWorkbenchRequests.Traveler(
                "Traveler",
                "Male",
                birthDate,
                "trav_" + travelerNumber,
                passengerTypeCode,
                personName,
                List.of(telephone),
                contactEmail != null ? List.of(new TravelportWorkbenchRequests.Email(contactEmail)) : null,
                List.of(travelDocument));
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

    /**
     * Searches Travelport Stays properties and maps each property's lowest available rate onto our
     * canonical {@link HotelOffer}. The city autocomplete a traveler picks from carries no
     * IATA-style code, only a name, so whenever {@link HotelSearchCriteria} carries resolved
     * coordinates (see {@code HotelSearchService}) the geo-location variant of the request
     * ({@link TravelportHotelGeoSearchRequest}, {@code SearchByGeoLocation}) is used instead of the
     * city-name-based {@link TravelportHotelSearchRequest} - falling back to the latter only when no
     * coordinates were resolved. Room type is not returned by the property search (it comes from a
     * follow-up availability step), so it is left blank here.
     */
    private List<HotelOffer> callHotelApi(HotelSearchCriteria criteria) {
        Object requestBody = criteria.latitude() != null && criteria.longitude() != null
                ? buildGeoSearchRequest(criteria)
                : buildCitySearchRequest(criteria);

        TravelportHotelSearchResponse response = restClient.post()
                .uri("/hotel/search/properties/search")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                .header(ACCESS_GROUP_HEADER, config.getAccessGroup())
                .header(PCC_HEADER, config.getPseudoCityCode())
                .accept(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(TravelportHotelSearchResponse.class);

        log.debug("[Travelport] hotel search response for {}: {}", criteria.cityCode(), response);

        var body = response == null ? null : response.PropertiesResponse();
        if (body == null || body.Properties() == null || body.Properties().PropertyInfo() == null) {
            return List.of();
        }

        String searchIdentifier = body.Properties().Identifier() != null ? body.Properties().Identifier().value() : null;
        return body.Properties().PropertyInfo().stream()
                .map(info -> toHotelOffer(info, criteria, searchIdentifier))
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Fetches the next page of an already-run hotel search ({@code GET
     * /hotel/search/properties/{identifier}?pageNumber=N}), reusing the same
     * {@link TravelportHotelSearchResponse} shape as the initial POST search since this is meant
     * to be the same result set, just paged. {@code searchIdentifier} is only ever passed in by
     * {@code HotelSearchService} when Travelport actually captured one on page 1 (see
     * {@link #callHotelApi}), so a blank/missing one here would be a caller bug, not a "no more
     * pages" signal - guarded anyway since this must never throw for the caller.
     */
    @Override
    public List<HotelOffer> loadMoreHotels(HotelSearchCriteria criteria, String searchIdentifier, int pageNumber) {
        if (config.isMockMode() || searchIdentifier == null || searchIdentifier.isBlank()) {
            return List.of();
        }

        TravelportHotelSearchResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/hotel/search/properties/{identifier}")
                            .queryParam("pageNumber", pageNumber)
                            .build(searchIdentifier))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                    .header(ACCESS_GROUP_HEADER, config.getAccessGroup())
                    .header(PCC_HEADER, config.getPseudoCityCode())
                    .header("TVP-Correlation-Id", UUID.randomUUID().toString())
                    .header("TraceId", "HotelLoadMore_" + UUID.randomUUID())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(TravelportHotelSearchResponse.class);
        } catch (RestClientException e) {
            log.warn("[Travelport] load more hotels failed for page {} of search {}: {}",
                    pageNumber, searchIdentifier, e.getMessage());
            return List.of();
        }

        log.debug("[Travelport] hotel load-more response for search {} page {}: {}", searchIdentifier, pageNumber, response);

        var body = response == null ? null : response.PropertiesResponse();
        if (body == null || body.Properties() == null || body.Properties().PropertyInfo() == null) {
            return List.of();
        }

        // The response carrying the same search's identifier lets a later page keep paginating
        // even if Travelport reissues/rotates it - falls back to the one we called with otherwise.
        String nextIdentifier = body.Properties().Identifier() != null
                ? body.Properties().Identifier().value() : searchIdentifier;
        return body.Properties().PropertyInfo().stream()
                .map(info -> toHotelOffer(info, criteria, nextIdentifier))
                .filter(Objects::nonNull)
                .toList();
    }

    private TravelportHotelGeoSearchRequest buildGeoSearchRequest(HotelSearchCriteria criteria) {
        var guests = new TravelportHotelGeoSearchRequest.RoomStayCandidate(
                "RoomStayCandidate",
                new TravelportHotelGeoSearchRequest.GuestCounts("GuestCounts", List.of(
                        new TravelportHotelGeoSearchRequest.GuestCount(
                                "GuestCount", null, Math.max(criteria.adults(), 1), "10"))));
        var query = new TravelportHotelGeoSearchRequest.PropertiesQuerySearch(
                "PropertiesQuerySearch",
                criteria.checkIn().toString(),
                criteria.checkOut().toString(),
                criteria.currency() != null ? criteria.currency() : "EUR",
                List.of(guests),
                new TravelportHotelGeoSearchRequest.SearchBy("SearchByGeoLocation",
                        new TravelportHotelGeoSearchRequest.SearchRadius(25, "Kilometers"),
                        criteria.latitude(), criteria.longitude()),
                true);
        return new TravelportHotelGeoSearchRequest(query);
    }

    private TravelportHotelSearchRequest buildCitySearchRequest(HotelSearchCriteria criteria) {
        var guests = new TravelportHotelSearchRequest.RoomStayCandidate(
                "RoomStayCandidate",
                new TravelportHotelSearchRequest.GuestCounts("GuestCounts", List.of(
                        new TravelportHotelSearchRequest.GuestCount("GuestCount", Math.max(criteria.adults(), 1), "10"))));
        var query = new TravelportHotelSearchRequest.PropertiesQuerySearch(
                "PropertiesQuerySearch",
                criteria.checkIn().toString(),
                criteria.checkOut().toString(),
                criteria.currency() != null ? criteria.currency() : "EUR",
                List.of(guests),
                new TravelportHotelSearchRequest.SearchBy("SearchByCity",
                        new TravelportHotelSearchRequest.SearchRadius(25, "Kilometers"), criteria.cityCode()),
                true);
        return new TravelportHotelSearchRequest(query);
    }

    private HotelOffer toHotelOffer(TravelportHotelSearchResponse.PropertyInfo info, HotelSearchCriteria criteria,
                                     String searchIdentifier) {
        if (info == null || info.Property() == null || info.LowestAvailableRate() == null
                || info.LowestAvailableRate().value() == null || info.LowestAvailableRate().code() == null) {
            return null;
        }
        var property = info.Property();
        String offerId = info.id() != null ? info.id()
                : property.PropertyKey() != null
                        ? property.PropertyKey().chainCode() + property.PropertyKey().propertyCode() : property.id();
        double rating = property.Rating() != null && !property.Rating().isEmpty()
                && property.Rating().get(0).value() != null ? property.Rating().get(0).value() : 0.0;

        Map<String, String> context = new java.util.HashMap<>();
        if (property.PropertyKey() != null) {
            if (property.PropertyKey().chainCode() != null) {
                context.put("chainCode", property.PropertyKey().chainCode());
            }
            if (property.PropertyKey().propertyCode() != null) {
                context.put("propertyCode", property.PropertyKey().propertyCode());
            }
        }
        context.put("adults", String.valueOf(Math.max(criteria.adults(), 1)));
        if (searchIdentifier != null && !searchIdentifier.isBlank()) {
            context.put("searchIdentifier", searchIdentifier);
        }

        String coverImageUrl = property.Image() != null && !property.Image().isEmpty()
                ? property.Image().get(0).value() : null;

        return new HotelOffer(
                getType(),
                offerId,
                property.name(),
                criteria.cityCode(),
                "",
                criteria.checkIn(),
                criteria.checkOut(),
                new Money(BigDecimal.valueOf(info.LowestAvailableRate().value()), info.LowestAvailableRate().code()),
                rating,
                coverImageUrl,
                context);
    }

    /**
     * Re-checks a property's room rates via Hotel Availability before booking, keyed by the
     * chain/property codes captured from the search ({@link HotelOffer#context}). Returns the
     * lowest offering's fresh total as the verified price; availability is false when no offering
     * comes back. When the property key is missing (e.g. an offer that carried no context), the
     * availability call is skipped and the originally quoted price is trusted.
     */
    private HotelPriceVerification callHotelAvailabilityApi(HotelOffer offer, int roomQuantity) {
        String offerId = offer.providerOfferId();
        log.debug("Début de la vérification du prix hôtel pour offerId: {}, roomQuantity demandée: {}", offerId, roomQuantity);

        // 1. Validation du contexte
        if (offer.context("chainCode") == null || offer.context("propertyCode") == null) {
            log.warn("Impossible de vérifier l'offre {}: context 'chainCode' ou 'propertyCode' manquant", offerId);
            return new HotelPriceVerification(offerId, null, true, null);
        }

        int rooms = Math.max(roomQuantity, 1);

        // 2. Appel API Travelport
        log.debug("Appel de la disponibilité Travelport pour offerId: {} avec {} chambre(s)", offerId, rooms);
        TravelportHotelAvailabilityResponse response = fetchHotelAvailability(offer, rooms);

        var offerings = response == null || response.CatalogOfferingsHospitalityResponse() == null
                ? null : response.CatalogOfferingsHospitalityResponse().CatalogOfferings();

        if (offerings == null || offerings.CatalogOffering() == null || offerings.CatalogOffering().isEmpty()) {
            log.warn("Aucune offre d'hébergement renvoyée par Travelport pour offerId: {}", offerId);
            return new HotelPriceVerification(offerId, null, false, null);
        }

        // 3. Extraction de l'offre la moins chère
        var cheapest = offerings.CatalogOffering().stream()
                .filter(o -> o.Price() != null && o.Price().TotalPrice() != null)
                .min((a, b) -> Double.compare(a.Price().TotalPrice(), b.Price().TotalPrice()))
                .orElse(null);

        if (cheapest == null) {
            log.warn("Aucune offre avec un tarif valide n'a été trouvée pour offerId: {}", offerId);
            return new HotelPriceVerification(offerId, null, true, null);
        }

        // 4. Calcul du nouveau prix par chambre
        String currency = cheapest.Price().CurrencyCode() != null && cheapest.Price().CurrencyCode().value() != null
                ? cheapest.Price().CurrencyCode().value() : offer.price().currency();

        BigDecimal totalPrice = BigDecimal.valueOf(cheapest.Price().TotalPrice());
        Money freshPrice = new Money(
                totalPrice.divide(BigDecimal.valueOf(rooms), 10, RoundingMode.HALF_UP), currency);

        log.info("Vérification de prix réussie pour offerId: {}. Prix d'origine: {}, Nouveaux prix recalculé par chambre: {} (Prix total API: {} {}, nombre de chambres: {})",
                offerId, offer.price(), freshPrice, totalPrice, currency, rooms);

        return new HotelPriceVerification(offerId, freshPrice, true, null);
    }

    /**
     * Fetches every bookable room/rate for the property via the same Stays Hotel Availability call
     * ({@code POST /hotel/availability/catalogofferingshospitality}) used for the price re-check,
     * this time keeping every {@code CatalogOffering} instead of just the cheapest one. Returns an
     * empty list when the offer carries no chain/property context or the property has no
     * availability, rather than calling with incomplete parameters.
     */
    private List<RoomOffer> callRoomOffersApi(HotelOffer offer) {
        if (offer.context("chainCode") == null || offer.context("propertyCode") == null) {
            return List.of();
        }

        // Room browsing (not a firm price check) always queries a single room; only the
        // price-verification and reservation paths need the guest's actual room count.
        TravelportHotelAvailabilityResponse response = fetchHotelAvailability(offer, 1);
        var offerings = response == null || response.CatalogOfferingsHospitalityResponse() == null
                ? null : response.CatalogOfferingsHospitalityResponse().CatalogOfferings();
        if (offerings == null || offerings.CatalogOffering() == null || offerings.CatalogOffering().isEmpty()) {
            return List.of();
        }

        return offerings.CatalogOffering().stream()
                .map(this::toRoomOffer)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private TravelportHotelAvailabilityResponse fetchHotelAvailability(HotelOffer offer, int roomQuantity) {
        String chainCode = offer.context("chainCode");
        String propertyCode = offer.context("propertyCode");

        int adults = 1;
        try {
            adults = Math.max(Integer.parseInt(offer.context("adults") != null ? offer.context("adults") : "1"), 1);
        } catch (NumberFormatException ignored) {
            // fall back to a single guest
        }
        int rooms = Math.max(roomQuantity, 1);
        var roomStayCandidate = new TravelportHotelAvailabilityRequest.RoomStayCandidate(
                "RoomStayCandidate",
                new TravelportHotelAvailabilityRequest.GuestCounts("GuestCounts", List.of(
                        new TravelportHotelAvailabilityRequest.GuestCount("GuestCount", adults))));
        var guests = new TravelportHotelAvailabilityRequest.RoomStayCandidates(
                "RoomStayCandidates", Collections.nCopies(rooms, roomStayCandidate));
        var request = new TravelportHotelAvailabilityRequest(
                new TravelportHotelAvailabilityRequest.CatalogOfferingsQueryRequest(
                        List.of(new TravelportHotelAvailabilityRequest.CatalogOfferingsRequest(
                                "CatalogOfferingsRequestHospitality",
                                true,
                                offer.price().currency(),
                                new TravelportHotelAvailabilityRequest.StayDates(
                                        offer.checkIn().toString(), offer.checkOut().toString()),
                                new TravelportHotelAvailabilityRequest.HotelSearchCriterion(
                                        "HotelSearchCriterion",
                                        1,
                                        List.of(new TravelportHotelAvailabilityRequest.PropertyRequest(
                                                "PropertyRequest",
                                                new TravelportHotelAvailabilityRequest.PropertyKey(
                                                        "PropertyKey", chainCode, propertyCode))),
                                        guests)))));

        TravelportHotelAvailabilityResponse response;
        try {
            response = restClient.post()
                    .uri("/hotel/availability/catalogofferingshospitality")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                    .header(ACCESS_GROUP_HEADER, config.getAccessGroup())
                    .header(PCC_HEADER, config.getPseudoCityCode())
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(TravelportHotelAvailabilityResponse.class);
        } catch (RestClientException e) {
            throw new ProviderException("Travelport hotel availability check failed for "
                    + offer.providerOfferId() + ": " + e.getMessage());
        }
        log.debug("[Travelport] hotel availability response for {}: {}", offer.providerOfferId(), response);
        return response;
    }

    private RoomOffer toRoomOffer(TravelportHotelAvailabilityResponse.CatalogOffering catalogOffering) {
        var productOptions = catalogOffering.ProductOptions();
        var product = productOptions == null || productOptions.isEmpty() || productOptions.get(0).Product() == null
                || productOptions.get(0).Product().isEmpty() ? null : productOptions.get(0).Product().get(0);
        var roomType = product == null ? null : product.RoomType();
        var characteristics = roomType == null ? null : roomType.RoomCharacteristics();
        var terms = catalogOffering.TermsAndConditions();
        var price = catalogOffering.Price();

        String roomTypeName = roomType != null && roomType.Description() != null
                ? roomType.Description().value() : null;
        String currency = price != null && price.CurrencyCode() != null ? price.CurrencyCode().value() : null;
        BigDecimal netPrice = price != null && price.TotalPrice() != null
                ? BigDecimal.valueOf(price.TotalPrice()) : null;

        List<String> facilities = characteristics == null || characteristics.RoomAmenity() == null ? List.of()
                : characteristics.RoomAmenity().stream()
                        .map(TravelportHotelAvailabilityResponse.RoomAmenity::description)
                        .filter(Objects::nonNull)
                        .toList();

        String boardType = terms != null && terms.MealsIncluded() != null
                && Boolean.TRUE.equals(terms.MealsIncluded().breakfastInd()) ? "Breakfast Included" : null;

        String cancellationPolicy = terms == null || terms.CancelPenalty() == null || terms.CancelPenalty().isEmpty()
                ? null
                : terms.CancelPenalty().stream()
                        .map(TravelportHotelAvailabilityResponse.CancelPenalty::Description)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(" "));

        String rateBasisId = terms == null || terms.ProductRateCodeInfo() == null || terms.ProductRateCodeInfo().isEmpty()
                ? null
                : terms.ProductRateCodeInfo().stream()
                        .map(TravelportHotelAvailabilityResponse.ProductRateCodeInfo::RateCodeInfo)
                        .filter(Objects::nonNull)
                        .map(TravelportHotelAvailabilityResponse.RateCodeInfo::rateCategory)
                        .filter(Objects::nonNull)
                        .findFirst().orElse(null);

        String description = terms != null && terms.Description() != null && !terms.Description().isEmpty()
                ? String.join(" ", terms.Description()) : roomTypeName;

        return new RoomOffer(
                catalogOffering.id(),
                roomTypeName,
                description,
                product == null ? null : product.bookingCode(),
                terms == null ? null : terms.RatePaymentInfo(),
                rateBasisId,
                currency,
                netPrice,
                boardType,
                null,
                null,
                cancellationPolicy,
                List.of(),
                facilities
        );
    }

    /**
     * Fetches full property content (photos, amenities, geo-coordinates, room-count breakdown) via
     * the Stays "Get Property Details" call ({@code GET /hotel/search/propertiesdetail}), keyed by
     * the chain/property codes captured from the search ({@link HotelOffer#context}). Returns
     * {@code null} when that context is missing (e.g. an offer harmonized without it) rather than
     * calling with incomplete parameters.
     */
    private HotelDetail callHotelDetailApi(HotelOffer offer) {
        String chainCode = offer.context("chainCode");
        String propertyCode = offer.context("propertyCode");
        if (chainCode == null || propertyCode == null) {
            return null;
        }

        TravelportHotelDetailResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/hotel/search/propertiesdetail")
                            .queryParam("chainCode", chainCode)
                            .queryParam("propertyCode", propertyCode)
                            .queryParam("ImageSize", "Large")
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                    .header(ACCESS_GROUP_HEADER, config.getAccessGroup())
                    .header(PCC_HEADER, config.getPseudoCityCode())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(TravelportHotelDetailResponse.class);
        } catch (RestClientException e) {
            throw new ProviderException("Travelport hotel detail lookup failed for " + chainCode + propertyCode
                    + ": " + e.getMessage());
        }
        log.debug("[Travelport] hotel detail response for {}{}: {}", chainCode, propertyCode, response);

        var propertiesResponse = response == null ? null : response.PropertiesResponse();
        var properties = propertiesResponse == null ? null : propertiesResponse.Properties();
        var propertyInfo = properties == null || properties.PropertyInfo() == null || properties.PropertyInfo().isEmpty()
                ? null : properties.PropertyInfo().get(0);
        var property = propertyInfo == null ? null : propertyInfo.Property();

        return property == null ? null : toHotelDetail(property);
    }

    private HotelDetail toHotelDetail(TravelportHotelDetailResponse.Property property) {
        var address = property.Address();
        String addressText = address != null && address.AddressLine() != null
                ? String.join(", ", address.AddressLine()) : null;
        String city = address != null ? address.City() : null;
        String country = address == null || address.Country() == null ? null
                : address.Country().name() != null ? address.Country().name() : address.Country().value();
        String postalCode = address != null && address.PostalCode() != null ? address.PostalCode().trim() : null;
        String email = property.Email() != null ? property.Email().value() : null;
        String phone = property.Telephone() != null && !property.Telephone().isEmpty()
                ? property.Telephone().get(0) : null;
        Double latitude = property.GeoLocation() != null ? property.GeoLocation().latitude() : null;
        Double longitude = property.GeoLocation() != null ? property.GeoLocation().longitude() : null;
        Double rating = property.Rating() != null && !property.Rating().isEmpty()
                ? property.Rating().get(0).value() : null;

        List<String> facilities = new java.util.ArrayList<>();
        if (property.PropertyAmenity() != null) {
            property.PropertyAmenity().forEach(a -> {
                if (a.description() != null) {
                    facilities.add(a.description());
                }
            });
        }
        if (property.BusinessService() != null) {
            property.BusinessService().forEach(s -> {
                if (s.description() != null) {
                    facilities.add(s.description());
                }
            });
        }
        if (property.AccessibilityFeature() != null) {
            property.AccessibilityFeature().forEach(f -> {
                if (f.description() != null) {
                    facilities.add(f.description());
                }
            });
        }

        List<HotelDetail.HotelImage> images = property.Image() == null ? List.of()
                : property.Image().stream()
                        .map(img -> new HotelDetail.HotelImage(img.caption(), img.value()))
                        .toList();

        String hotelId = property.PropertyKey() != null
                ? property.PropertyKey().chainCode() + property.PropertyKey().propertyCode() : property.id();

        return new HotelDetail(
                hotelId,
                property.name(),
                addressText,
                city,
                country,
                email,
                phone,
                postalCode,
                latitude,
                longitude,
                rating,
                null,
                facilities,
                images,
                null);
    }

    /**
     * Books a room via the Stays Create Reservation full-payload call
     * ({@code POST /hotel/book/reservations}), sending the cached offer, guest(s), and the
     * guarantee {@code FormOfPayment} Travelport requires on every hotel reservation (the actual
     * customer payment is still collected separately through our own PaymentGateway - this is
     * only what Travelport itself demands to hold the room). The response's
     * {@code Identifier.value} is used as the reservation reference. Travelport returns no explicit
     * hold deadline for hotels here, so a conservative 24h policy default is applied.
     */
    private ProviderBookingConfirmation callHotelReservationApi(HotelBookingRequest request) {
        HotelOffer offer = request.offer();
        List<PassengerInfo> guests = request.guests();
        List<TravelportHotelReservationRequest.Traveler> travelers = java.util.stream.IntStream.range(0, guests.size())
                .mapToObj(i -> toHotelTraveler(guests.get(i), request.contactEmail()))
                .toList();

        int roomQuantity = Math.max(request.quantity(), 1);
        String bookingCode = offer.context("bookingCode") != null ? offer.context("bookingCode")
                : offer.providerOfferId();
        var product = new TravelportHotelReservationRequest.Product(
                "ProductHospitality",
                bookingCode,
                String.valueOf(roomQuantity),
                Math.max(guests.size(), 1),
                new TravelportHotelReservationRequest.PropertyKey(
                        "PropertyKey", offer.context("propertyCode"), offer.context("chainCode")),
                new TravelportHotelReservationRequest.DateRange(
                        offer.checkIn().toString(), offer.checkOut().toString()));

        // offer.price() is per-room; Travelport expects the total across every room being booked.
        var price = new TravelportHotelReservationRequest.PriceDetail(
                "PriceDetail",
                new TravelportHotelReservationRequest.CurrencyCode(offer.price().currency()),
                null, null, offer.price().multiply(roomQuantity).amount().doubleValue());

        // La grille tarifaire (rateID) n'est capturée qu'à l'étape disponibilité (getRoomOffers) et
        // n'est pas encore propagée jusqu'à l'offre mise en cache ; on l'omet donc tant qu'elle
        // n'est pas disponible plutôt que d'envoyer une valeur inventée.
        String rateId = offer.context("rateID");
        List<TravelportHotelReservationRequest.TermsAndConditionsFull> termsAndConditions = rateId == null ? null
                : List.of(new TravelportHotelReservationRequest.TermsAndConditionsFull(
                        List.of(new TravelportHotelReservationRequest.ProductRateCodeInfo(
                                "ProductRateCodeInfo",
                                new TravelportHotelReservationRequest.RateCodeInfo(rateId, null)))));

        var reservationDetail = new TravelportHotelReservationRequest.ReservationDetail(
                List.of(new TravelportHotelReservationRequest.Offer(
                        "Offer",
                        new TravelportHotelReservationRequest.Identifier("TVPT"),
                        List.of(product),
                        price,
                        termsAndConditions)),
                List.of(new TravelportHotelReservationRequest.Payment(
                        "Payment",
                        new TravelportHotelReservationRequest.Amount(offer.price().currency(), offer.price().amount().doubleValue()),
                        false,
                        true)),
                List.of(buildGuaranteeFormOfPayment()),
                travelers);

        TravelportReservationResponse response;
        try {
            response = withBookingRetry("hotel reservation", () -> bookingRestClient.post()
                    .uri("/hotel/book/reservations")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                    .header(ACCESS_GROUP_HEADER, config.getAccessGroup())
                    .header(PCC_HEADER, config.getPseudoCityCode())
                    .accept(MediaType.APPLICATION_JSON)
                    .body(new TravelportHotelReservationRequest(reservationDetail))
                    .retrieve()
                    .body(TravelportReservationResponse.class));
        } catch (RestClientException e) {
            throw new ProviderException("Travelport hotel reservation failed: " + e.getMessage());
        }
        log.debug("[Travelport] hotel reservation response for offer {}: {}", offer.providerOfferId(), response);

        String confirmation = confirmationFrom(response);
        if (confirmation == null) {
            throw new ProviderException("Travelport hotel reservation failed: " + commitFailureReason(response));
        }
        return new ProviderBookingConfirmation(getType(), confirmation, LocalDateTime.now().plusHours(24), true);
    }

    /**
     * Builds the company's own guarantee/virtual card FormOfPayment Travelport requires on every
     * hotel reservation, sourced entirely from config/.env (see
     * {@link ProviderProperties.Vendor#getGuaranteeCardNumber()} and siblings) - never a customer's
     * card, which our own PaymentGateway collects separately.
     */
    private TravelportHotelReservationRequest.FormOfPayment buildGuaranteeFormOfPayment() {
        var address = new TravelportHotelReservationRequest.Address(
                "AddressDetail",
                null,
                config.getGuaranteeCardBillingStreet(),
                null,
                config.getGuaranteeCardBillingCity(),
                null,
                new TravelportHotelReservationRequest.StateProv(config.getGuaranteeCardBillingStateProv(), null),
                new TravelportHotelReservationRequest.Country(config.getGuaranteeCardBillingCountry(), null),
                config.getGuaranteeCardBillingPostalCode());

        var telephone = new TravelportHotelReservationRequest.Telephone(
                "TelephoneDetail", null, null, config.getGuaranteeCardBillingPhone(), null);

        var paymentCard = new TravelportHotelReservationRequest.PaymentCard(
                "PaymentCardDetail",
                config.getGuaranteeCardExpireDate(),
                config.getGuaranteeCardType(),
                config.getGuaranteeCardCode(),
                config.getGuaranteeCardHolderName(),
                new TravelportHotelReservationRequest.CardNumber("CardNumber", config.getGuaranteeCardNumber()),
                new TravelportHotelReservationRequest.SeriesCode("SeriesCode", config.getGuaranteeCardSeriesCode()),
                null,
                address,
                List.of(telephone),
                List.of(new TravelportHotelReservationRequest.Email(config.getGuaranteeCardBillingEmail())));

        return new TravelportHotelReservationRequest.FormOfPayment("FormOfPaymentPaymentCard", paymentCard);
    }

    private TravelportHotelReservationRequest.Traveler toHotelTraveler(PassengerInfo passenger, String contactEmail) {
        String[] nameParts = splitName(passenger.fullName());
        var personName = new TravelportHotelReservationRequest.TravelerPersonName(
                "PersonName", nameParts[0], nameParts[1], null);
        var telephone = new TravelportHotelReservationRequest.Telephone(
                "TelephoneDetail", "237", null, "670000000", "DLA");
        return new TravelportHotelReservationRequest.Traveler(
                "Traveler",
                personName,
                List.of(telephone),
                contactEmail != null ? List.of(new TravelportHotelReservationRequest.Email(contactEmail)) : null);
    }
}
