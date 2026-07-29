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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    private final ProviderProperties.Vendor config;
    private final RestClient restClient;
    private final TravelportTokenProvider tokenProvider;
    private final ObjectMapper objectMapper;

    public TravelportClient(RestClient.Builder restClientBuilder, ProviderProperties properties,
                             ObjectMapper objectMapper) {
        this.config = properties.getTravelport();
        this.objectMapper = objectMapper;
        ClientHttpRequestFactorySettings timeoutSettings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(config.getTimeoutMillis()))
                .withReadTimeout(Duration.ofMillis(config.getTimeoutMillis()));
        this.restClient = restClientBuilder
                .baseUrl(config.getBaseUrl().isBlank() ? "https://api.pp.travelport.net/11" : config.getBaseUrl())
                .requestFactory(ClientHttpRequestFactories.get(timeoutSettings))
                .build();
        this.tokenProvider = new TravelportTokenProvider(RestClient.builder(), config);
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
    public HotelPriceVerification verifyHotelPrice(HotelOffer offer) {
        if (config.isMockMode()) {
            return ProviderMockSupport.verifyHotelPrice(offer.providerOfferId());
        }
        return callHotelAvailabilityApi(offer);
    }

    @Override
    public HotelDetail getDetailHotel(HotelOffer offer) {
        return null;
    }

    @Override
    public List<RoomOffer> getRoomOffers(HotelOffer offer) {
        return List.of();
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
            restClient.delete()
                    .uri(RESERVATIONS_BASE + "/{locator}", pnrCode)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                    .header(ACCESS_GROUP_HEADER, config.getAccessGroup())
                    .header(PCC_HEADER, config.getPseudoCityCode())
                    .retrieve()
                    .toBodilessEntity();
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
        log.info("[Travelport] flight search request for {}->{}: {}", criteria.origin(), criteria.destination(),
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
        log.info("[Travelport] price request for offer {}: {}", offer.providerOfferId(), writeAsJson(request));

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
        log.info("[Travelport] price response for offer {}: {}", offer.providerOfferId(), response);

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
        String session = UUID.randomUUID().toString();

        newWorkbench(session, new TravelportWorkbenchRequests.Reservation("Reservation", null, null, null, null));

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
        String uri = issueTicket
                ? RESERVATIONS_BASE + "/{session}?autoDeleteDate={autoDeleteDate}&Issuance=Ticket&DocumentValue=Retain&payLaterInd=false"
                : RESERVATIONS_BASE + "/{session}?autoDeleteDate={autoDeleteDate}&DocumentValue=Retain&payLaterInd=true";
        var body = new TravelportCommitRequest(true, true, true, false, true, true,
                "AcceptOfferPriceDifference", "GUENS TRAVEL", true);
        TravelportReservationResponse response;
        try {
            response = restClient.post()
                    .uri(uri, session, autoDeleteDate)
                    .headers(h -> workbenchHeaders(h, session))
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(TravelportReservationResponse.class);
        } catch (RestClientException e) {
            throw new ProviderException("Travelport commit failed: " + e.getMessage());
        }
        log.info("[Travelport] commit response (issueTicket={}) for session {}: {}", issueTicket, session, response);
        return response;
    }

    private String confirmationFrom(TravelportReservationResponse response) {
        var body = response == null ? null : response.ReservationResponse();
        if (body == null) {
            return null;
        }
        boolean ok = "Success".equalsIgnoreCase(body.reservationStatus())
                || (body.Result() != null && "Complete".equalsIgnoreCase(body.Result().status()));
        if (!ok || body.Identifier() == null) {
            return null;
        }
        return body.Identifier().value();
    }

    private String commitFailureReason(TravelportReservationResponse response) {
        var result = response == null || response.ReservationResponse() == null
                ? null : response.ReservationResponse().Result();
        if (result != null && result.Error() != null && !result.Error().isEmpty()) {
            return result.Error().get(0).Message();
        }
        return "commit did not return a successful reservation";
    }

    /** New Workbench: {@code POST /air/book/session/reservationworkbench} with a Reservation body. */
    private void newWorkbench(String session, TravelportWorkbenchRequests.Reservation reservation) {
        try {
            restClient.post()
                    .uri(WORKBENCH_BASE)
                    .headers(h -> workbenchHeaders(h, session))
                    .accept(MediaType.APPLICATION_JSON)
                    .body(reservation)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new ProviderException("Travelport new workbench failed: " + e.getMessage());
        }
    }

    /**
     * Add Offer (reference payload): adds the searched offer to the workbench by its identifiers,
     * matching a real production reference client's request shape (see
     * {@link TravelportAddOfferRequest}'s Javadoc). Uses the CatalogProductOfferings container id +
     * Identifier and the priced product's {@code productRef} captured from the Search response
     * ({@link FlightOffer#context}) when available, falling back to the offering id / container
     * identifier in every position the reference itself falls back on when it lacks finer-grained
     * ids for this step.
     */
    private void addOffer(String session, FlightOffer offer) {
        String offeringId = offer.providerOfferId();
        String containerId = offer.context("catalogOfferingsId") != null
                ? offer.context("catalogOfferingsId") : offeringId;
        String authority = offer.context("identifierAuthority") != null
                ? offer.context("identifierAuthority") : TVPT_AUTHORITY;
        String catalogOfferingsIdentifierValue = offer.context("catalogOfferingsIdentifier");
        var containerIdentifier = catalogOfferingsIdentifierValue != null
                ? new TravelportAddOfferRequest.Identifier(catalogOfferingsIdentifierValue, authority) : null;
        var offeringIdentifier = offer.context("offeringIdentifier") != null
                ? new TravelportAddOfferRequest.Identifier(offer.context("offeringIdentifier"), authority)
                : containerIdentifier;
        var productBrandOfferingIdentifier = containerIdentifier;

        String productRef = offer.context("productRef");
        List<TravelportAddOfferRequest.ProductIdentifier> productIdentifiers = productRef != null
                ? List.of(new TravelportAddOfferRequest.ProductIdentifier("product_" + productRef, "product_" + productRef,
                        new TravelportAddOfferRequest.Identifier(productRef, authority)))
                : null;

        int segmentCount = 1;
        try {
            if (offer.context("segmentCount") != null) {
                segmentCount = Integer.parseInt(offer.context("segmentCount"));
            }
        } catch (NumberFormatException ignored) {
            // keep the 1-segment default
        }
        List<Integer> segmentSequence = java.util.stream.IntStream.rangeClosed(1, segmentCount).boxed().toList();

        var request = new TravelportAddOfferRequest(
                new TravelportAddOfferRequest.OfferQueryBuildFromCatalogProductOfferings(
                        "OfferQueryBuildFromCatalogProductOfferings",
                        new TravelportAddOfferRequest.PaymentCriteria("PaymentCriteria", true, true, true, true),
                        new TravelportAddOfferRequest.BuildFromCatalogProductOfferingsRequest(
                                "BuildFromCatalogProductOfferingsRequestAir",
                                new TravelportAddOfferRequest.OfferingsRef(containerId, containerIdentifier),
                                List.of(new TravelportAddOfferRequest.CatalogProductOfferingSelection(
                                        "CatalogProductOfferingSelection",
                                        new TravelportAddOfferRequest.OfferingRef(offeringId, offeringIdentifier, offeringId),
                                        productBrandOfferingIdentifier,
                                        productIdentifiers,
                                        segmentSequence))),
                        4));
        log.info("[Travelport] add offer request for {}: {}", offeringId, writeAsJson(request));

        TravelportOfferListResponse response;
        try {
            response = restClient.post()
                    .uri("{base}/{session}/offers/buildfromcatalogproductofferings", WORKBENCH_AIROFFER_BASE, session)
                    .headers(h -> workbenchHeaders(h, session))
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(TravelportOfferListResponse.class);

        } catch (RestClientException e) {
            throw new ProviderException("Travelport add offer failed for " + offeringId + ": " + e.getMessage());
        }
        log.info("[Travelport] add offer response for {}: {}", offeringId, response);

        boolean added = response != null && response.OfferListResponse() != null
                && response.OfferListResponse().OfferID() != null
                && !response.OfferListResponse().OfferID().isEmpty();
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
        try {
            restClient.post()
                    .uri("{base}/{session}/travelers", WORKBENCH_TRAVELER_BASE, session)
                    .headers(h -> workbenchHeaders(h, session))
                    .accept(MediaType.APPLICATION_JSON)
                    .body(traveler)
                    .retrieve()
                    .toBodilessEntity();
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
        try {
            restClient.post()
                    .uri(WORKBENCH_BASE + "/buildfromlocator?Locator={locator}", locator)
                    .headers(h -> workbenchHeaders(h, session))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toBodilessEntity();
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
        TravelportFormOfPaymentResponse response;
        try {
            response = restClient.post()
                    .uri("{base}/{session}/formofpayment?authorizePaymentInd=true", PAYMENT_BASE, session)
                    .headers(h -> workbenchHeaders(h, session))
                    .accept(MediaType.APPLICATION_JSON)
                    .body(fop)
                    .retrieve()
                    .body(TravelportFormOfPaymentResponse.class);
        } catch (RestClientException e) {
            throw new ProviderException("Travelport add form of payment failed: " + e.getMessage());
        }
        log.info("[Travelport] form of payment response for session {}: {}", session, response);

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
        try {
            restClient.post()
                    .uri("{base}/{session}/payments", PAYMENT_OFFER_BASE, session)
                    .headers(h -> workbenchHeaders(h, session))
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
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

        var travelDocument = new TravelportWorkbenchRequests.TravelDocument(
                "TravelDocumentDetail",
                passenger.passportNumber() != null ? passenger.passportNumber().toUpperCase() : "N0000000",
                "Passport",
                passenger.passportExpiryDate() != null ? passenger.passportExpiryDate().toString()
                        : LocalDate.now().plusYears(3).toString(),
                passenger.passportIssueCountry() != null ? passenger.passportIssueCountry().toUpperCase() : "CM",
                birthDate,
                "Male");

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
     * Searches Travelport Stays properties by IATA city code and maps each property's lowest
     * available rate onto our canonical {@link HotelOffer}. Room type is not returned by the
     * property search (it comes from a follow-up availability step), so it is left blank here.
     */
    private List<HotelOffer> callHotelApi(HotelSearchCriteria criteria) {
        var guests = new TravelportHotelSearchRequest.RoomStayCandidate(
                new TravelportHotelSearchRequest.GuestCounts("GuestCounts", List.of(
                        new TravelportHotelSearchRequest.GuestCount("GuestCount", Math.max(criteria.adults(), 1), "10"))));
        var query = new TravelportHotelSearchRequest.PropertiesQuerySearch(
                "PropertiesQuerySearch",
                criteria.checkIn().toString(),
                criteria.checkOut().toString(),
                criteria.currency() != null ? criteria.currency() : "EUR",
                List.of(guests),
                new TravelportHotelSearchRequest.SearchBy("SearchByCityCode", criteria.cityCode(),
                        new TravelportHotelSearchRequest.SearchRadius(25, "Kilometers")),
                true);

        TravelportHotelSearchResponse response = restClient.post()
                .uri("/hotel/search/properties/search")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                .header(ACCESS_GROUP_HEADER, config.getAccessGroup())
                .header(PCC_HEADER, config.getPseudoCityCode())
                .accept(MediaType.APPLICATION_JSON)
                .body(new TravelportHotelSearchRequest(query))
                .retrieve()
                .body(TravelportHotelSearchResponse.class);

        log.info("[Travelport] hotel search response for {}: {}", criteria.cityCode(), response);

        var body = response == null ? null : response.PropertiesResponse();
        if (body == null || body.Properties() == null || body.Properties().PropertyInfo() == null) {
            return List.of();
        }

        return body.Properties().PropertyInfo().stream()
                .map(info -> toHotelOffer(info, criteria))
                .filter(Objects::nonNull)
                .toList();
    }

    private HotelOffer toHotelOffer(TravelportHotelSearchResponse.PropertyInfo info, HotelSearchCriteria criteria) {
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
                context);
    }

    /**
     * Re-checks a property's room rates via Hotel Availability before booking, keyed by the
     * chain/property codes captured from the search ({@link HotelOffer#context}). Returns the
     * lowest offering's fresh total as the verified price; availability is false when no offering
     * comes back. When the property key is missing (e.g. an offer that carried no context), the
     * availability call is skipped and the originally quoted price is trusted.
     */
    private HotelPriceVerification callHotelAvailabilityApi(HotelOffer offer) {
        String chainCode = offer.context("chainCode");
        String propertyCode = offer.context("propertyCode");
        if (chainCode == null || propertyCode == null) {
            return new HotelPriceVerification(offer.providerOfferId(), null, true, null);
        }

        var request = new TravelportHotelAvailabilityRequest(
                new TravelportHotelAvailabilityRequest.CatalogOfferingsQueryRequest(
                        "CatalogOfferingsRequestHospitality",
                        List.of(new TravelportHotelAvailabilityRequest.CatalogOfferingsRequest(
                                "CatalogOfferingsRequestHospitality",
                                offer.price().currency(),
                                new TravelportHotelAvailabilityRequest.StayDates(
                                        offer.checkIn().toString(), offer.checkOut().toString()),
                                new TravelportHotelAvailabilityRequest.HotelSearchCriterion(
                                        "HotelSearchCriterion",
                                        1,
                                        List.of(new TravelportHotelAvailabilityRequest.PropertyRequest(
                                                "PropertyRequest",
                                                new TravelportHotelAvailabilityRequest.PropertyKey(chainCode, propertyCode))))))));

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
        log.info("[Travelport] hotel availability response for {}: {}", offer.providerOfferId(), response);

        var offerings = response == null || response.CatalogOfferingsHospitalityResponse() == null
                ? null : response.CatalogOfferingsHospitalityResponse().CatalogOfferings();
        if (offerings == null || offerings.CatalogOffering() == null || offerings.CatalogOffering().isEmpty()) {
            return new HotelPriceVerification(offer.providerOfferId(), null, false, null);
        }

        var cheapest = offerings.CatalogOffering().stream()
                .filter(o -> o.Price() != null && o.Price().TotalPrice() != null)
                .min((a, b) -> Double.compare(a.Price().TotalPrice(), b.Price().TotalPrice()))
                .orElse(null);
        if (cheapest == null) {
            return new HotelPriceVerification(offer.providerOfferId(), null, true, null);
        }

        String currency = cheapest.Price().CurrencyCode() != null && cheapest.Price().CurrencyCode().value() != null
                ? cheapest.Price().CurrencyCode().value() : offer.price().currency();
        Money freshPrice = new Money(BigDecimal.valueOf(cheapest.Price().TotalPrice()), currency);
        return new HotelPriceVerification(offer.providerOfferId(), freshPrice, true, null);
    }

    /**
     * Books a room via the Stays Create Reservation full-payload call
     * ({@code POST /hotel/book/reservations}), sending the cached offer plus the guest(s). No
     * form of payment is sent here (our own PaymentGateway collects the funds); the response's
     * {@code Identifier.value} is used as the reservation reference. Travelport returns no explicit
     * hold deadline for hotels here, so a conservative 24h policy default is applied.
     */
    private ProviderBookingConfirmation callHotelReservationApi(HotelBookingRequest request) {
        List<PassengerInfo> guests = request.guests();
        List<TravelportWorkbenchRequests.Traveler> travelers = java.util.stream.IntStream.range(0, guests.size())
                .mapToObj(i -> toWorkbenchTraveler(guests.get(i), request.contactEmail(), null, i + 1))
                .toList();
        var reservation = new TravelportWorkbenchRequests.Reservation(
                "Reservation",
                List.of(new TravelportWorkbenchRequests.Offer(
                        "Offer", request.offer().providerOfferId(), request.offer().providerOfferId(), null, "GDS")),
                travelers,
                null,
                null);

        TravelportReservationResponse response;
        try {
            response = restClient.post()
                    .uri("/hotel/book/reservations")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                    .header(ACCESS_GROUP_HEADER, config.getAccessGroup())
                    .header(PCC_HEADER, config.getPseudoCityCode())
                    .accept(MediaType.APPLICATION_JSON)
                    .body(new TravelportHotelReservationRequest(reservation))
                    .retrieve()
                    .body(TravelportReservationResponse.class);
        } catch (RestClientException e) {
            throw new ProviderException("Travelport hotel reservation failed: " + e.getMessage());
        }
        log.info("[Travelport] hotel reservation response for offer {}: {}", request.offer().providerOfferId(), response);

        String confirmation = confirmationFrom(response);
        if (confirmation == null) {
            throw new ProviderException("Travelport hotel reservation failed: " + commitFailureReason(response));
        }
        return new ProviderBookingConfirmation(getType(), confirmation, LocalDateTime.now().plusHours(24), true);
    }
}
