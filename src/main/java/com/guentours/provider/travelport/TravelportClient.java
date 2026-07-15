package com.guentours.provider.travelport;

import com.guentours.provider.FinalHotelConfirmation;
import com.guentours.provider.FinalTicketConfirmation;
import com.guentours.provider.FlightBookingRequest;
import com.guentours.provider.FlightOffer;
import com.guentours.provider.FlightSearchCriteria;
import com.guentours.provider.HotelBookingRequest;
import com.guentours.provider.HotelOffer;
import com.guentours.provider.HotelSearchCriteria;
import com.guentours.provider.JourneyType;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
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
    private static final List<String> HOTELS = List.of("Hotel Le Meridien", "Ibis Central", "Travelport Resort");

    private final ProviderProperties.Vendor config;
    private final RestClient restClient;
    private final TravelportTokenProvider tokenProvider;

    public TravelportClient(RestClient.Builder restClientBuilder, ProviderProperties properties) {
        this.config = properties.getTravelport();
        ClientHttpRequestFactorySettings timeoutSettings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(config.getTimeoutMillis()))
                .withReadTimeout(Duration.ofMillis(config.getTimeoutMillis()));
        this.restClient = restClientBuilder
                .baseUrl(config.getBaseUrl().isBlank() ? "https://api.pp.travelport.com/11" : config.getBaseUrl())
                .requestFactory(ClientHttpRequestFactories.get(timeoutSettings))
                .build();
        this.tokenProvider = new TravelportTokenProvider(RestClient.builder(), config);
        log.info(this.tokenProvider.toString());
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
        restClient.delete()
                .uri("/book/reservation/reservations/{locator}", pnrCode)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                .header(ACCESS_GROUP_HEADER, config.getAccessGroup())
                .header(PCC_HEADER, config.getPseudoCityCode())
                .retrieve()
                .toBodilessEntity();
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
        log.info("TOKEN ",tokenProvider);
        TravelportSearchResponse response = restClient.post()
                .uri("/air/catalog/search/catalogproductofferings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                .header(ACCESS_GROUP_HEADER, config.getAccessGroup())
                .header(PCC_HEADER, config.getPseudoCityCode())
                .header(SESSION_HEADER, UUID.randomUUID().toString())
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TravelportSearchResponse.class);

        var body = response == null ? null : response.CatalogProductOfferingsResponse();
        if (body == null || body.CatalogProductOfferings() == null
                || body.CatalogProductOfferings().CatalogProductOffering() == null) {
            return List.of();
        }

        Map<String, TravelportSearchResponse.Flight> flightsById = flightsById(body);
        var offerings = body.CatalogProductOfferings();

        return offerings.CatalogProductOffering().stream()
                .map(offering -> toFlightOffer(offerings, offering, flightsById, criteria))
                .filter(Objects::nonNull)
                .toList();
    }

    /** Captures the Search identifiers the Add Offer reference payload needs back at booking time. */
    private Map<String, String> bookingContext(TravelportSearchResponse.CatalogProductOfferings offerings,
                                               TravelportSearchResponse.CatalogProductOffering offering) {
        Map<String, String> context = new java.util.HashMap<>();
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

        var passengers = new java.util.ArrayList<TravelportSearchRequest.PassengerCriteria>();
        passengers.add(new TravelportSearchRequest.PassengerCriteria("PassengerCriteria", Math.max(criteria.adults(), 1), null, "ADT"));
        if (criteria.children() > 0) {
            passengers.add(new TravelportSearchRequest.PassengerCriteria("PassengerCriteria", criteria.children(), null, "CNN"));
        }
        if (criteria.infants() > 0) {
            passengers.add(new TravelportSearchRequest.PassengerCriteria("PassengerCriteria", criteria.infants(), null, "INF"));
        }

        var request = new TravelportSearchRequest.CatalogProductOfferingsRequest(
                "CatalogProductOfferingsRequestAir",
                1,
                15,
                List.of("GDS"),
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
                                       FlightSearchCriteria criteria) {
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
                bookingContext(offerings, offering));
    }

    private Money extractPrice(TravelportSearchResponse.ProductBrandOptions brandOption, String fallbackCurrency) {
        if (brandOption.ProductBrandOffering() == null || brandOption.ProductBrandOffering().isEmpty()) {
            return null;
        }
        var priced = brandOption.ProductBrandOffering().get(0).Price();
        if (priced == null || priced.TotalPrice() == null) {
            return null;
        }
        String currency = priced.CurrencyCode() != null ? priced.CurrencyCode() : fallbackCurrency;
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
     * {@link TravelportPriceRequest}'s Javadoc for the search-context caveat.
     */
    private FlightPriceVerification callPriceApi(FlightOffer offer) {
        var request = new TravelportPriceRequest(new TravelportPriceRequest.PriceProductsQueryRequest(
                "PriceProductsQueryRequest",
                List.of(new TravelportPriceRequest.CatalogProductOfferingSelection(
                        "CatalogProductOfferingSelection",
                        new TravelportPriceRequest.Identifier(offer.providerOfferId())))));

        TravelportPriceResponse response = restClient.post()
                .uri("/price/offers/buildfromcatalogproductofferings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                .header(ACCESS_GROUP_HEADER, config.getAccessGroup())
                .header(PCC_HEADER, config.getPseudoCityCode())
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TravelportPriceResponse.class);

        var pricedOffer = response == null || response.OffersResponse() == null
                || response.OffersResponse().Offer() == null || response.OffersResponse().Offer().isEmpty()
                ? null : response.OffersResponse().Offer().get(0);
        if (pricedOffer == null || pricedOffer.Price() == null || pricedOffer.Price().TotalPrice() == null) {
            return new FlightPriceVerification(offer.providerOfferId(), null, false, 0, null);
        }

        String currency = pricedOffer.Price().CurrencyCode() != null
                ? pricedOffer.Price().CurrencyCode() : offer.price().currency();
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

        for (com.guentours.provider.PassengerInfo passenger : request.passengers()) {
            addTraveler(session, toWorkbenchTraveler(passenger, request.contactEmail()));
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
     * Workbench Commit ({@code POST /air/book/reservation/reservations/{workbenchId}}). With no
     * payment in the workbench it books and creates the PNR; with {@code Issuance=Ticket} (payment
     * present) it issues the tickets.
     */
    private TravelportReservationResponse commit(String session, boolean issueTicket) {
        String uri = issueTicket
                ? RESERVATIONS_BASE + "/{session}?Issuance=Ticket&DocumentValue=Retain"
                : RESERVATIONS_BASE + "/{session}";
        return restClient.post()
                .uri(uri, session)
                .headers(h -> workbenchHeaders(h, session))
                .accept(MediaType.APPLICATION_JSON)
                .body(new TravelportCommitRequest("GUENTOURS", true, true))
                .retrieve()
                .body(TravelportReservationResponse.class);
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
        restClient.post()
                .uri(WORKBENCH_BASE)
                .headers(h -> workbenchHeaders(h, session))
                .accept(MediaType.APPLICATION_JSON)
                .body(reservation)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Add Offer (reference payload): adds the searched offer to the workbench by its identifiers.
     * Uses the CatalogProductOfferings container id + Identifier and the offering Identifier captured
     * from the Search response ({@link FlightOffer#context}) when available, falling back to the
     * offering id in every position for offers that carried no such context.
     */
    private void addOffer(String session, FlightOffer offer) {
        String offeringId = offer.providerOfferId();
        String containerId = offer.context("catalogOfferingsId") != null
                ? offer.context("catalogOfferingsId") : offeringId;
        String authority = offer.context("identifierAuthority");
        var containerIdentifier = offer.context("catalogOfferingsIdentifier") != null
                ? new TravelportAddOfferRequest.Identifier(offer.context("catalogOfferingsIdentifier"), authority) : null;
        var offeringIdentifier = offer.context("offeringIdentifier") != null
                ? new TravelportAddOfferRequest.Identifier(offer.context("offeringIdentifier"), authority) : null;

        var request = new TravelportAddOfferRequest(
                "OfferQueryBuildFromCatalogProductOfferings",
                new TravelportAddOfferRequest.BuildFromCatalogProductOfferingsRequest(
                        "BuildFromCatalogProductOfferingsRequestAir",
                        new TravelportAddOfferRequest.OfferingsRef(containerId, containerIdentifier),
                        List.of(new TravelportAddOfferRequest.CatalogProductOfferingSelection(
                                "CatalogProductOfferingSelection",
                                new TravelportAddOfferRequest.OfferingRef(offeringId, offeringIdentifier, offeringId),
                                null))),
                4);

        TravelportOfferListResponse response = restClient.post()
                .uri("{base}/{session}/offers/buildfromcatalogproductofferings", WORKBENCH_AIROFFER_BASE, session)
                .headers(h -> workbenchHeaders(h, session))
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TravelportOfferListResponse.class);

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
        restClient.post()
                .uri("{base}/{session}/travelers", WORKBENCH_TRAVELER_BASE, session)
                .headers(h -> workbenchHeaders(h, session))
                .accept(MediaType.APPLICATION_JSON)
                .body(traveler)
                .retrieve()
                .toBodilessEntity();
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
        restClient.post()
                .uri(WORKBENCH_BASE + "/buildfromlocator?Locator={locator}", locator)
                .headers(h -> workbenchHeaders(h, session))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Add Form of Payment: adds a cash FOP to the workbench, our internal transaction reference in
     * {@code FreeText}. {@code POST /air/payment/reservationworkbench/{session}/formofpayment}.
     * Returns the FOP reference (id/ref) for the subsequent Add Payment step, or {@code null}.
     */
    private String addFormOfPayment(String session, PaymentDetails payment) {
        var fop = new TravelportFormOfPaymentRequest(
                "FormOfPaymentCash", true, true, null, "GuenTours txn " + payment.transactionReference());
        TravelportFormOfPaymentResponse response = restClient.post()
                .uri("{base}/{session}/formofpayment?authorizePaymentInd=true", PAYMENT_BASE, session)
                .headers(h -> workbenchHeaders(h, session))
                .accept(MediaType.APPLICATION_JSON)
                .body(fop)
                .retrieve()
                .body(TravelportFormOfPaymentResponse.class);

        var created = response == null || response.FormOfPaymentResponse() == null
                ? null : response.FormOfPaymentResponse().FormOfPayment();
        if (created == null) {
            return null;
        }
        return created.FormOfPaymentRef() != null ? created.FormOfPaymentRef() : created.id();
    }

    /**
     * Add Payment: applies the workbench form of payment to the offer(s) so the following commit
     * issues the ticket(s). {@code POST /air/paymentoffer/reservationworkbench/{session}/payments}.
     */
    private void addPayment(String session, PaymentDetails payment, String fopRef) {
        var request = new TravelportPaymentRequest(
                "Payment",
                new TravelportPaymentRequest.Amount(
                        payment.amount().amount().doubleValue(), payment.amount().currency()),
                new TravelportPaymentRequest.FormOfPaymentIdentifier("FormOfPaymentPaymentCash", fopRef, fopRef));
        restClient.post()
                .uri("{base}/{session}/payments", PAYMENT_OFFER_BASE, session)
                .headers(h -> workbenchHeaders(h, session))
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    private void workbenchHeaders(HttpHeaders headers, String session) {
        headers.setBearerAuth(tokenProvider.getAccessToken());
        headers.set(ACCESS_GROUP_HEADER, config.getAccessGroup());
        headers.set(PCC_HEADER, config.getPseudoCityCode());
        headers.set(SESSION_HEADER, session);
    }

    private TravelportWorkbenchRequests.Traveler toWorkbenchTraveler(com.guentours.provider.PassengerInfo passenger,
                                                                     String contactEmail) {
        String[] nameParts = splitName(passenger.fullName());
        String passengerTypeCode = switch (passenger.type()) {
            case ADULT -> "ADT";
            case CHILD -> "CNN";
            case INFANT -> "INF";
        };
        var personName = new TravelportWorkbenchRequests.PersonName(
                "PersonNameDetail", null, nameParts[0], null, nameParts[1]);
        return new TravelportWorkbenchRequests.Traveler(
                "Traveler",
                passenger.dateOfBirth() != null ? passenger.dateOfBirth().toString() : null,
                passengerTypeCode,
                personName,
                contactEmail != null ? List.of(new TravelportWorkbenchRequests.Email(contactEmail)) : null);
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
                "EUR",
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

        TravelportHotelAvailabilityResponse response = restClient.post()
                .uri("/hotel/availability/catalogofferingshospitality")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                .header(ACCESS_GROUP_HEADER, config.getAccessGroup())
                .header(PCC_HEADER, config.getPseudoCityCode())
                .accept(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(TravelportHotelAvailabilityResponse.class);

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
        List<TravelportWorkbenchRequests.Traveler> travelers = request.guests().stream()
                .map(g -> toWorkbenchTraveler(g, request.contactEmail()))
                .toList();
        var reservation = new TravelportWorkbenchRequests.Reservation(
                "Reservation",
                List.of(new TravelportWorkbenchRequests.Offer(
                        "Offer", request.offer().providerOfferId(), request.offer().providerOfferId(), null, "GDS")),
                travelers,
                null,
                null);

        TravelportReservationResponse response = restClient.post()
                .uri("/hotel/book/reservations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                .header(ACCESS_GROUP_HEADER, config.getAccessGroup())
                .header(PCC_HEADER, config.getPseudoCityCode())
                .accept(MediaType.APPLICATION_JSON)
                .body(new TravelportHotelReservationRequest(reservation))
                .retrieve()
                .body(TravelportReservationResponse.class);

        String confirmation = confirmationFrom(response);
        if (confirmation == null) {
            throw new ProviderException("Travelport hotel reservation failed: " + commitFailureReason(response));
        }
        return new ProviderBookingConfirmation(getType(), confirmation, LocalDateTime.now().plusHours(24), true);
    }
}
