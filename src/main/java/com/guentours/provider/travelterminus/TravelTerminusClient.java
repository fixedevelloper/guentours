package com.guentours.provider.travelterminus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guentours.provider.*;
import com.guentours.provider.dto.FlightPriceVerification;
import com.guentours.provider.dto.HotelPriceVerification;
import com.guentours.provider.travelterminus.dto.*;
import com.guentours.shared.Money;
import com.guentours.shared.exception.ProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Adapter for the Travel Terminus flight API (docs: supplier-api.travelterminus.com), a
 * flights-only NDC-style aggregator. Two things make this adapter shaped differently from
 * Travelopro/Sabre/Travelport:
 *
 * <p><b>Search is Server-Sent Events, not a plain request/response.</b> {@code GET
 * /api/flights/search-stream} streams {@code route_update} events until the connection closes;
 * since our SPI's {@link #searchFlights} is synchronous we simply read the whole body as text
 * (blocking until Travel Terminus closes the stream) and parse it line by line - handling both
 * the real SSE framing ({@code event:}/{@code id:}/{@code data: <json>}) and the raw
 * newline-delimited JSON shown in the vendor's own sample code, since the two disagree.
 *
 * <p><b>There is no "hold" state.</b> Travel Terminus retired Hold booking on 2026-08-13: {@code
 * POST /api/flights/book} now debits the vendor wallet and issues the e-ticket immediately, in
 * one call. Our own flow calls {@link #createFlightHold} *before* the customer's payment is
 * captured and only issues the ticket in {@link #issueFlightTicket} afterwards - calling Book
 * that early would spend the wallet before we know the customer will actually pay. So
 * {@link #createFlightHold} only runs Branded Fare + Revalidate (locks price/inventory, spends
 * nothing) and remembers the result in {@link #pendingBookings} under a synthetic PNR; the real
 * Book call is deferred to {@link #issueFlightTicket}, once payment is in hand.
 */
@Component
public class TravelTerminusClient implements TravelProviderClient {

    private static final Logger log = LoggerFactory.getLogger(TravelTerminusClient.class);

    private static final List<String> AIRLINES = List.of("6E", "AI", "EK", "UK");
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; GuenToursAPI/1.0)";
    private static final int DEFAULT_SEATS_AVAILABLE = 9;
    private static final long TOKEN_REFRESH_BUFFER_SECONDS = 300;
    private static final int PENDING_BOOKING_TTL_MINUTES = 15;
    private static final String PENDING_BOOKING_PREFIX = "TTH-";
    private static final int ORDER_DETAILS_POLL_ATTEMPTS = 3;
    private static final long ORDER_DETAILS_POLL_DELAY_MS = 1500;
    // Our canonical PassengerInfo carries no gender/title field (only fullName, dob, passport,
    // nationality) - same gap TravelportClient.toWorkbenchTraveler hit, same fix: default until a
    // real gender/title field exists in the domain model.
    private static final String DEFAULT_GENDER = "M";
    private static final String DEFAULT_TITLE = "MR";
    // Book rejects a booking with "passengers.0.City is required." (real Stage sandbox behavior -
    // not flagged as mandatory in the docs' field table) and PassengerInfo carries no city either,
    // same gap as gender/title above.
    private static final String DEFAULT_CITY = "N/A";
    // The docs' hand-written example shows a 12h clock with AM/PM ("3:55 PM"), but the real Stage
    // sandbox returns a plain 24h clock ("03:00", "21:25") - yet another Travel Terminus doc/reality
    // mismatch (see the search-stream fare field names for another one), so both are tried.
    private static final DateTimeFormatter AIRPORT_TIME_FORMAT_24H = DateTimeFormatter.ofPattern("H:mm", Locale.ENGLISH);
    private static final DateTimeFormatter AIRPORT_TIME_FORMAT_12H = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final ProviderProperties.Vendor config;
    /** Token generation + Streaming Search, using {@code timeoutMillis}. */
    private final RestClient restClient;
    /** Branded Fare / Revalidate / Book / Order Details / Cancel - the booking-flow calls, using
     *  the longer {@code bookingTimeoutMillis} (same reasoning as the other adapters: these can
     *  legitimately take longer on the vendor side than a search call). */
    private final RestClient bookingRestClient;
    private final ObjectMapper objectMapper;

    private volatile String cachedAccessToken;
    private volatile long tokenExpiresAtEpochSecond;

    /** Price/inventory locked via Branded Fare + Revalidate at {@link #createFlightHold} time,
     *  keyed by the synthetic PNR handed back to the caller, consumed by {@link #issueFlightTicket}
     *  once payment is captured. In-memory only: an app restart between hold and payment loses the
     *  pending entry (issueFlightTicket then reports {@code issued=false}) - acceptable given the
     *  short {@link #PENDING_BOOKING_TTL_MINUTES} window; a persistent store would only be worth
     *  adding if that is observed to matter operationally. */
    private final Map<String, PendingBooking> pendingBookings = new ConcurrentHashMap<>();

    public TravelTerminusClient(RestClient.Builder restClientBuilder, ProviderProperties properties, ObjectMapper objectMapper) {
        this.config = properties.getTravelterminus();
        this.objectMapper = objectMapper;

        long connectTimeout = 10_000L;
        long searchReadTimeout = config.getTimeoutMillis() > 0 ? config.getTimeoutMillis() : 30_000L;
        long bookingReadTimeout = config.getBookingTimeoutMillis() > 0 ? config.getBookingTimeoutMillis() : 60_000L;

        this.restClient = restClientBuilder.clone()
                .baseUrl(config.getBaseUrl())
                .requestFactory(ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(Duration.ofMillis(connectTimeout))
                        .withReadTimeout(Duration.ofMillis(searchReadTimeout))))
                .build();

        this.bookingRestClient = restClientBuilder.clone()
                .baseUrl(config.getBaseUrl())
                .requestFactory(ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(Duration.ofMillis(connectTimeout))
                        .withReadTimeout(Duration.ofMillis(bookingReadTimeout))))
                .build();
    }

    @Override
    public ProviderType getType() {
        return ProviderType.TRAVELTERMINUS;
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

    // ==========================================
    // Search
    // ==========================================

    @Override
    public List<FlightOffer> searchFlights(FlightSearchCriteria criteria) {
        if (!isEnabled()) {
            return List.of();
        }
        log.info("[TravelTerminus] searchFlights: {}", criteria);
        try {
            List<FlightOffer> offers = config.isMockMode() ? ProviderMockSupport.flights(getType(), criteria, AIRLINES, 1.0)
                    : callSearchStream(criteria);
            log.info("[TravelTerminus] searchFlights: returning {} offer(s) for {} -> {} on {}",
                    offers.size(), criteria.origin(), criteria.destination(), criteria.departureDate());
            return offers;
        } catch (Exception ex) {
            log.warn("Travel Terminus flight search failed, skipping this provider: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<FlightOffer> callSearchStream(FlightSearchCriteria criteria) {
        List<SearchLeg> legs = buildLegs(criteria);
        List<SearchPax> paxes = List.of(
                new SearchPax("ADT", Math.max(1, criteria.adults())),
                new SearchPax("CHD", criteria.children()),
                new SearchPax("INF", criteria.infants()));
        TravelPreferences prefs = new TravelPreferences(mapCabinClass(criteria.cabinClass()),
                criteria.journeyType() == JourneyType.ROUND_TRIP && legs.size() > 1 ? "roundtrip" : "oneway");
        String currency = criteria.currency() == null ? "USD" : criteria.currency();

        String legsJson = writeJson(legs);
        String paxJson = writeJson(paxes);
        String prefsJson = writeJson(prefs);

        log.info("[TravelTerminus] search-stream request: searchAirLegs={}, paxes={}, travelPreferences={}, currency={}",
                legsJson, paxJson, prefsJson, currency);

        // The query param values are raw JSON (containing literal '{'/'}'). Passing them straight
        // into queryParam(...).build() makes UriComponentsBuilder mistake those braces for its own
        // URI template placeholder syntax ("Not enough variable values available to expand..."), so
        // each value is instead passed as a placeholder + supplied through build(Object...), which
        // substitutes and percent-encodes it as an opaque literal instead of re-parsing it.
        String body = withAuth(token -> restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/flights/search-stream")
                        .queryParam("searchAirLegs", "{searchAirLegs}")
                        .queryParam("paxes", "{paxes}")
                        .queryParam("travelPreferences", "{travelPreferences}")
                        .queryParam("endUserIP", "{endUserIP}")
                        .queryParam("endUserBrowserAgent", "{endUserBrowserAgent}")
                        .build(legsJson, paxJson, prefsJson, config.getClientIp(), USER_AGENT))
                .header("access-token", token)
                .header("accept", "application/json")
                .header("version", "1")
                .header("Currency-Preference", currency)
                .header("language", "en")
                .retrieve()
                .body(String.class));

        log.info("[TravelTerminus] search-stream raw response ({} chars): {}",
                body == null ? 0 : body.length(), body);

        return parseSearchStream(body, criteria);
    }

    private List<FlightOffer> parseSearchStream(String body, FlightSearchCriteria criteria) {
        if (body == null || body.isBlank()) {
            log.info("[TravelTerminus] search-stream returned an empty body, no offers to parse");
            return List.of();
        }
        List<FlightOffer> offers = new ArrayList<>();
        String searchReqId = null;
        String hashReqKey = null;
        for (String rawLine : body.split("\n")) {
            String line = rawLine.strip();
            if (line.isEmpty() || line.startsWith("event:") || line.startsWith("id:") || line.startsWith(":")) {
                continue;
            }
            if (line.startsWith("data:")) {
                line = line.substring("data:".length()).strip();
            }
            if (line.isEmpty()) {
                continue;
            }
            JsonNode node;
            try {
                node = objectMapper.readTree(line);
            } catch (IOException e) {
                log.debug("[TravelTerminus] Skipping unparseable search-stream line: {}", line);
                continue;
            }
            if (!"route_update".equals(node.path("type").asText(""))) {
                continue;
            }
            JsonNode searchResponseNode = node.path("data").path("searchResponse");
            if (searchResponseNode.isMissingNode()) {
                continue;
            }
            TravelTerminusSearchResponse response;
            try {
                response = objectMapper.treeToValue(searchResponseNode, TravelTerminusSearchResponse.class);
            } catch (IOException e) {
                log.warn("[TravelTerminus] Failed to map a route_update event: {}", e.getMessage());
                continue;
            }
            if (!"success".equals(response.status()) || response.route() == null) {
                continue;
            }
            if (searchReqId == null) {
                searchReqId = response.searchReqId();
                hashReqKey = response.hashReqKey();
            }
            String currency = response.meta() != null ? response.meta().currency() : criteria.currency();
            for (TravelTerminusRoute route : response.route()) {
                FlightOffer offer = toFlightOffer(route, criteria, searchReqId, hashReqKey, currency);
                if (offer != null) {
                    offers.add(offer);
                }
            }
        }
        log.info("[TravelTerminus] search-stream parsed {} offer(s) (searchReqId={})", offers.size(), searchReqId);
        return offers;
    }

    /**
     * Maps one itinerary to our canonical, single-leg {@link FlightOffer}. For a round-trip route
     * (two leg-arrays in {@code flightSegments}), only the outbound leg's origin/destination/times
     * are surfaced here - the return leg is real and priced together with the outbound in the same
     * fare, but our canonical shape has no return-leg fields. The full round-trip itinerary is
     * preserved as-is in {@code providerContext}'s opaque {@code flightObject} and is what actually
     * gets booked, so nothing is lost for booking purposes - only for what this summary displays.
     */
    private FlightOffer toFlightOffer(TravelTerminusRoute route, FlightSearchCriteria criteria,
                                       String searchReqId, String hashReqKey, String fallbackCurrency) {
        if (route.flightSegments() == null || route.flightSegments().isEmpty()
                || route.flightSegments().get(0).isEmpty() || route.fare() == null || route.fare().isEmpty()) {
            return null;
        }
        List<TravelTerminusSegment> outbound = route.flightSegments().get(0);
        TravelTerminusSegment firstSegment = outbound.get(0);
        TravelTerminusSegment lastSegment = outbound.get(outbound.size() - 1);
        if (firstSegment.departure() == null || firstSegment.departure().isEmpty()
                || lastSegment.arrival() == null || lastSegment.arrival().isEmpty()) {
            return null;
        }
        var departurePoint = firstSegment.departure().get(0);
        var arrivalPoint = lastSegment.arrival().get(0);

        LocalDateTime departureTime = parseAirportDateTime(departurePoint.date(), departurePoint.time());
        LocalDateTime arrivalTime = parseAirportDateTime(arrivalPoint.date(), arrivalPoint.time());
        if (departureTime == null || arrivalTime == null) {
            return null;
        }

        TravelTerminusFare fare = route.fare().get(0);
        if (fare.totalFare() == null) {
            return null;
        }
        String currency = fare.branchCurrency() != null ? fare.branchCurrency() : fallbackCurrency;
        Money price = new Money(BigDecimal.valueOf(fare.totalFare()), currency == null ? "USD" : currency);

        String routeId = route.flightObject() != null && route.flightObject().hasNonNull("routeId")
                ? route.flightObject().get("routeId").asText()
                : UUID.randomUUID().toString();

        String flightObjectJson = writeJson(route.flightObject());
        if (flightObjectJson == null) {
            log.warn("[TravelTerminus] Could not serialize flightObject for route {}, skipping offer", routeId);
            return null;
        }
        Map<String, String> context = new HashMap<>();
        context.put("searchReqId", searchReqId);
        context.put("hashReqKey", hashReqKey);
        context.put("flightObject", flightObjectJson);

        FlightOfferDetail detail = new FlightOfferDetail(
                route.isHoldAvailable(),
                firstOrNull(route.totalDuration()),
                firstOrNull(route.totalInterval()),
                toSegmentDetails(outbound));

        return new FlightOffer(
                getType(),
                "TT-" + routeId,
                firstSegment.airlineCode(),
                firstSegment.airlineName(),
                firstSegment.airlineCode() + firstSegment.flightNumber(),
                departurePoint.code(),
                arrivalPoint.code(),
                departureTime,
                arrivalTime,
                firstSegment.cabinClass() != null ? firstSegment.cabinClass() : criteria.cabinClass(),
                price,
                DEFAULT_SEATS_AVAILABLE,
                context,
                detail
        );
    }

    /** One entry per physical flight segment of the leg - two or more means a stopover (see
     *  {@link FlightSegmentDetail#layoverAfter()}, sourced from {@code segmentInterval}). */
    private List<FlightSegmentDetail> toSegmentDetails(List<TravelTerminusSegment> segments) {
        List<FlightSegmentDetail> details = new ArrayList<>();
        for (TravelTerminusSegment segment : segments) {
            if (segment.departure() == null || segment.departure().isEmpty()
                    || segment.arrival() == null || segment.arrival().isEmpty()) {
                continue;
            }
            var departurePoint = segment.departure().get(0);
            var arrivalPoint = segment.arrival().get(0);
            details.add(new FlightSegmentDetail(
                    segment.airlineCode(),
                    segment.airlineName(),
                    segment.flightNumber(),
                    segment.cabinClass(),
                    toAirportInfo(departurePoint),
                    toAirportInfo(arrivalPoint),
                    parseAirportDateTime(departurePoint.date(), departurePoint.time()),
                    parseAirportDateTime(arrivalPoint.date(), arrivalPoint.time()),
                    segment.segmentDuration(),
                    segment.segmentInterval(),
                    toBaggageRules(segment.cabinBaggages()),
                    toBaggageRules(segment.checkInBaggages())));
        }
        return details;
    }

    private AirportInfo toAirportInfo(TravelTerminusSegment.AirportPoint point) {
        return new AirportInfo(point.code(), point.name(), point.city(), point.terminal());
    }

    private List<BaggageRule> toBaggageRules(List<TravelTerminusSegment.BaggageAllowance> allowances) {
        if (allowances == null) {
            return List.of();
        }
        return allowances.stream()
                .map(a -> new BaggageRule(a.paxType(), a.rule(), a.quantity(), a.size()))
                .toList();
    }

    private static String firstOrNull(List<String> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private LocalDateTime parseAirportDateTime(String date, String time) {
        if (date == null || time == null) {
            return null;
        }
        LocalTime parsedTime;
        try {
            parsedTime = LocalTime.parse(time.strip(), AIRPORT_TIME_FORMAT_24H);
        } catch (DateTimeParseException e24) {
            try {
                parsedTime = LocalTime.parse(time.strip().toUpperCase(Locale.ENGLISH), AIRPORT_TIME_FORMAT_12H);
            } catch (DateTimeParseException e12) {
                log.warn("[TravelTerminus] Failed to parse airport time '{}' (tried 24h and 12h formats): {}",
                        time, e12.getMessage());
                return null;
            }
        }
        try {
            return LocalDate.parse(date).atTime(parsedTime);
        } catch (DateTimeParseException e) {
            log.warn("[TravelTerminus] Failed to parse airport date '{}': {}", date, e.getMessage());
            return null;
        }
    }

    private List<SearchLeg> buildLegs(FlightSearchCriteria criteria) {
        if (criteria.journeyType() == JourneyType.ROUND_TRIP && criteria.returnDate() != null) {
            return List.of(
                    new SearchLeg(criteria.departureDate().toString(), criteria.origin(), criteria.destination()),
                    new SearchLeg(criteria.returnDate().toString(), criteria.destination(), criteria.origin()));
        }
        // Streaming Search only accepts a single leg (oneway) or an outbound+inbound pair
        // (roundtrip) - true multi-city is already handled upstream, one leg/search at a time (see
        // BookingService.completeMultiCityHold), so MULTI_CITY criteria are just a one-way leg here.
        return List.of(new SearchLeg(criteria.departureDate().toString(), criteria.origin(), criteria.destination()));
    }

    private String mapCabinClass(String cabinClass) {
        if (cabinClass == null) {
            return "economy";
        }
        return switch (cabinClass.toUpperCase(Locale.ENGLISH)) {
            case "PREMIUM_ECONOMY", "PREMIUM ECONOMY" -> "premium_economy";
            case "BUSINESS" -> "business";
            case "FIRST" -> "first";
            default -> "economy";
        };
    }

    // ==========================================
    // Price/availability verification (Branded Fare -> Revalidate)
    // ==========================================

    @Override
    public FlightPriceVerification verifyFlightPrice(FlightOffer offer) {
        if (config.isMockMode()) {
            return ProviderMockSupport.verifyFlightPrice(offer.providerOfferId());
        }
        RevalidateOutcome outcome = revalidate(offer);
        if (!outcome.valid() || outcome.route() == null) {
            throw new OfferExpiredException("This Travel Terminus flight offer is no longer available, please search again");
        }
        TravelTerminusRoute route = outcome.route();
        Money verifiedPrice = null;
        if (route.fare() != null && !route.fare().isEmpty() && route.fare().get(0).totalFare() != null) {
            String currency = route.fare().get(0).branchCurrency() != null
                    ? route.fare().get(0).branchCurrency() : offer.price().currency();
            verifiedPrice = new Money(BigDecimal.valueOf(route.fare().get(0).totalFare()), currency);
        }
        int seatsRemaining = minSeatsAvailable(route).orElse(offer.seatsAvailable());
        return new FlightPriceVerification(offer.providerOfferId(), verifiedPrice, seatsRemaining > 0, seatsRemaining, null);
    }

    private OptionalInt minSeatsAvailable(TravelTerminusRoute route) {
        if (route.flightSegments() == null) {
            return OptionalInt.empty();
        }
        return route.flightSegments().stream()
                .flatMap(List::stream)
                .map(TravelTerminusSegment::noOfSeatAvailable)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .min();
    }

    /**
     * Runs the mandatory Branded Fare -> Revalidate chain for {@code offer}, selecting the
     * cheapest tier per leg/group (see {@link #selectBrandedFareTiers}). Shared by
     * {@link #verifyFlightPrice} and {@link #createFlightHold}, which each call it independently
     * (rather than threading one result between them) since the SPI carries no channel from
     * {@code verifyFlightPrice}'s return value into {@code createFlightHold} - only the original
     * offer and passenger data reach the hold call, so re-running the chain there is both required
     * by the contract and the safer choice (freshest possible price/inventory at hold time).
     */
    private RevalidateOutcome revalidate(FlightOffer offer) {
        String searchReqId = offer.context("searchReqId");
        String hashReqKey = offer.context("hashReqKey");
        String flightObjectJson = offer.context("flightObject");
        if (searchReqId == null || hashReqKey == null || flightObjectJson == null) {
            log.warn("[TravelTerminus] Missing search context on offer {}, cannot revalidate", offer.providerOfferId());
            return RevalidateOutcome.invalid();
        }

        JsonNode flightObject;
        try {
            flightObject = objectMapper.readTree(flightObjectJson);
        } catch (IOException e) {
            log.warn("[TravelTerminus] Corrupt cached flightObject for offer {}: {}", offer.providerOfferId(), e.getMessage());
            return RevalidateOutcome.invalid();
        }

        TravelTerminusBrandedFareResponse brandedFare;
        try {
            TravelTerminusBrandedFareRequest brandedRequest = new TravelTerminusBrandedFareRequest(
                    searchReqId, hashReqKey, flightObject, config.getClientIp(), USER_AGENT);
            TravelTerminusEnvelope<TravelTerminusBrandedFareResponse> envelope = withAuth(token -> bookingRestClient.post()
                    .uri("/api/flights/branded-fare")
                    .header("access-token", token)
                    .header("accept", "*/*")
                    .header("language", "en")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(brandedRequest)
                    .retrieve()
                    .body(new ParameterizedTypeReference<TravelTerminusEnvelope<TravelTerminusBrandedFareResponse>>() {
                    }));
            brandedFare = envelope != null ? envelope.data() : null;
        } catch (RuntimeException e) {
            log.warn("[TravelTerminus] Branded fare lookup failed for offer {}: {}", offer.providerOfferId(), e.getMessage());
            return RevalidateOutcome.invalid();
        }
        if (brandedFare == null || !"success".equals(brandedFare.status())) {
            return RevalidateOutcome.invalid();
        }

        String revalidateSearchReqId = brandedFare.searchReqId() != null ? brandedFare.searchReqId() : searchReqId;
        String revalidateHashReqKey = brandedFare.hashReqKey() != null ? brandedFare.hashReqKey() : hashReqKey;
        TravelTerminusFlightObjectRef ref = new TravelTerminusFlightObjectRef(
                brandedFare.searchObject(), selectBrandedFareTiers(brandedFare.brandedFares()));

        TravelTerminusRevalidateResponse revalidateResponse;
        try {
            TravelTerminusRevalidateRequest revalidateRequest = new TravelTerminusRevalidateRequest(
                    revalidateSearchReqId, revalidateHashReqKey, ref, config.getClientIp(), USER_AGENT);
            TravelTerminusEnvelope<TravelTerminusRevalidateResponse> envelope = withAuth(token -> bookingRestClient.post()
                    .uri("/api/flights/revalidate")
                    .header("access-token", token)
                    .header("accept", "*/*")
                    .header("language", "en")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(revalidateRequest)
                    .retrieve()
                    .body(new ParameterizedTypeReference<TravelTerminusEnvelope<TravelTerminusRevalidateResponse>>() {
                    }));
            revalidateResponse = envelope != null ? envelope.data() : null;
        } catch (RuntimeException e) {
            log.warn("[TravelTerminus] Revalidate failed for offer {}: {}", offer.providerOfferId(), e.getMessage());
            return RevalidateOutcome.invalid();
        }

        if (revalidateResponse == null || !"success".equals(revalidateResponse.status()) || revalidateResponse.route() == null) {
            return RevalidateOutcome.invalid();
        }
        // Book must be called with the searchReqId/hashReqKey *returned by Revalidate*, not the
        // ones sent into it - the real Stage sandbox rejects Book with "Hash req key is mismatch"
        // otherwise, even though those are also valid-looking UUID/hash strings.
        String bookSearchReqId = revalidateResponse.searchReqId() != null ? revalidateResponse.searchReqId() : revalidateSearchReqId;
        String bookHashReqKey = revalidateResponse.hashReqKey() != null ? revalidateResponse.hashReqKey() : revalidateHashReqKey;
        return new RevalidateOutcome(true, revalidateResponse.route(), bookSearchReqId, bookHashReqKey);
    }

    /** See the "Preparing for Revalidate & Fare Rule" doc section: 0 entries for a Standard fare
     *  (no branded tiers), 1 for a combined round-trip, 2 for outbound+inbound priced separately.
     *  Always picks the cheapest tier per group/leg - upselling to a pricier branded tier is a UI
     *  concern out of scope for this v1 integration (no Pre-Ancillary/fare-tier selection SPI method). */
    private List<JsonNode> selectBrandedFareTiers(List<TravelTerminusBrandedFareResponse.Group> brandedFares) {
        if (brandedFares == null || brandedFares.isEmpty()) {
            return List.of();
        }
        List<JsonNode> selected = new ArrayList<>();
        for (TravelTerminusBrandedFareResponse.Group group : brandedFares) {
            if (group.fare() == null || group.fare().isEmpty()) {
                continue;
            }
            TravelTerminusBrandedFareResponse.Tier cheapest = group.fare().stream()
                    .min(Comparator.comparing(t -> t.totalFare() == null ? Double.MAX_VALUE : t.totalFare()))
                    .orElse(group.fare().get(0));
            if (cheapest.flightObject() != null) {
                selected.add(cheapest.flightObject());
            }
        }
        return selected;
    }

    // ==========================================
    // Pre Ancillary (optional priced baggage/meal/seat extras before Book)
    // ==========================================

    @Override
    public List<AncillaryOption> ancillaryOptions(FlightOffer offer, List<PassengerInfo> passengers) {
        if (config.isMockMode() || !isEnabled()) {
            return List.of();
        }
        RevalidateOutcome outcome = revalidate(offer);
        if (!outcome.valid() || outcome.route() == null || outcome.route().flightObject() == null) {
            log.warn("[TravelTerminus] Could not lock a price/flightObject for offer {} while fetching ancillary options",
                    offer.providerOfferId());
            return List.of();
        }
        // Passing an empty (rather than null) array when the caller has no passenger names yet
        // would fail routes where seatAncillaryRequiresPassengers is true - omitting the field
        // entirely lets Travel Terminus tell us via an error whether it was actually required.
        List<TravelTerminusPreAncillaryRequest.Passenger> paxList = passengers.isEmpty() ? null
                : passengers.stream().map(p -> {
                    String[] nameParts = splitName(p.fullName());
                    return new TravelTerminusPreAncillaryRequest.Passenger(
                            mapPassengerType(p.type()), DEFAULT_TITLE, nameParts[0], nameParts[1]);
                }).toList();

        TravelTerminusPreAncillaryResponse response;
        try {
            TravelTerminusPreAncillaryRequest request = new TravelTerminusPreAncillaryRequest(
                    outcome.searchReqId(), outcome.hashReqKey(), outcome.route().flightObject(),
                    config.getClientIp(), USER_AGENT, paxList);
            TravelTerminusEnvelope<TravelTerminusPreAncillaryResponse> envelope = withAuth(token -> bookingRestClient.post()
                    .uri("/api/flights/pre-ancillary")
                    .header("access-token", token)
                    .header("accept", "*/*")
                    .header("language", "en")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<TravelTerminusEnvelope<TravelTerminusPreAncillaryResponse>>() {
                    }));
            response = envelope != null ? envelope.data() : null;
        } catch (RuntimeException e) {
            log.warn("[TravelTerminus] Pre-ancillary lookup failed for offer {}: {}", offer.providerOfferId(), e.getMessage());
            return List.of();
        }
        return response == null ? List.of() : mapAncillaryOptions(response);
    }

    private List<AncillaryOption> mapAncillaryOptions(TravelTerminusPreAncillaryResponse response) {
        List<AncillaryOption> options = new ArrayList<>();
        if (response.baggages() != null) {
            for (TravelTerminusPreAncillaryResponse.BaggageGroup group : response.baggages()) {
                if (group.bagsData() == null) {
                    continue;
                }
                for (TravelTerminusPreAncillaryResponse.BagOption bag : group.bagsData()) {
                    addPaxOptions(options, AncillaryType.BAGGAGE, group.segmentId(), bag.baggageWeight(),
                            bag.baggageWeight(), bag.passengers(), null);
                }
            }
        }
        if (response.meals() != null) {
            for (TravelTerminusPreAncillaryResponse.MealGroup group : response.meals()) {
                if (group.mealsData() == null) {
                    continue;
                }
                for (TravelTerminusPreAncillaryResponse.MealOption meal : group.mealsData()) {
                    addPaxOptions(options, AncillaryType.MEAL, group.segmentId(), meal.code(), meal.description(),
                            meal.passengers(), null);
                }
            }
        }
        if (response.seats() != null) {
            for (TravelTerminusPreAncillaryResponse.SeatGroup group : response.seats()) {
                if (group.seatData() == null) {
                    continue;
                }
                for (TravelTerminusPreAncillaryResponse.SeatOption seat : group.seatData()) {
                    if (!"Available".equalsIgnoreCase(seat.status())) {
                        continue;
                    }
                    String seatCode = (seat.row() == null ? "" : seat.row()) + (seat.column() == null ? "" : seat.column());
                    SeatLayout seatLayout = new SeatLayout(seat.row(), seat.column(), seat.status(),
                            Boolean.TRUE.equals(seat.exitRow()), Boolean.TRUE.equals(seat.accessible()),
                            Boolean.TRUE.equals(seat.bassinet()), Boolean.TRUE.equals(seat.toilet()),
                            Boolean.TRUE.equals(seat.galley()),
                            group.totalRows() != null ? group.totalRows() : 0,
                            group.totalColumns() != null ? group.totalColumns() : 0,
                            group.seatGroups() != null ? group.seatGroups() : List.of(),
                            group.cabinClass());
                    addPaxOptions(options, AncillaryType.SEAT, group.segmentId(), seatCode, "Seat " + seatCode,
                            seat.passengers(), seatLayout);
                }
            }
        }
        return options;
    }

    /** One {@link AncillaryOption} per eligible passenger entry - each carries its own price and
     *  opaque {@code flightObject} token, serialized as-is into {@link AncillaryOption#providerToken()}
     *  so {@link #toBookPassenger} can echo it back unmodified at Book time. */
    private void addPaxOptions(List<AncillaryOption> options, AncillaryType type, String segmentId, String code,
                                String label, List<TravelTerminusPreAncillaryResponse.PaxPrice> paxPrices,
                                SeatLayout seatLayout) {
        if (paxPrices == null) {
            return;
        }
        for (TravelTerminusPreAncillaryResponse.PaxPrice paxPrice : paxPrices) {
            if (paxPrice.price() == null || paxPrice.currency() == null || paxPrice.flightObject() == null) {
                continue;
            }
            String providerToken = writeJson(paxPrice.flightObject());
            if (providerToken == null) {
                continue;
            }
            String paxRef = paxPrice.flightObject().path("paxRef").asText(null);
            options.add(new AncillaryOption(type, segmentId, code, label,
                    new Money(paxPrice.price(), paxPrice.currency()), paxRef, providerToken, seatLayout));
        }
    }

    // ==========================================
    // Booking (deferred Book: lock now, book+ticket at payment capture)
    // ==========================================

    @Override
    public ProviderBookingConfirmation createFlightHold(FlightBookingRequest request) {
        if (config.isMockMode()) {
            return ProviderMockSupport.flightHold(getType());
        }
        purgeExpiredPendingBookings();
        FlightOffer offer = request.offer();
        RevalidateOutcome outcome = revalidate(offer);
        if (!outcome.valid() || outcome.route() == null || outcome.route().flightObject() == null) {
            log.warn("[TravelTerminus] Could not lock a price/flightObject for offer {} while creating a hold",
                    offer.providerOfferId());
            return new ProviderBookingConfirmation(getType(), null, null, false);
        }

        String syntheticPnr = PENDING_BOOKING_PREFIX + UUID.randomUUID();
        LocalDateTime deadline = LocalDateTime.now().plusMinutes(PENDING_BOOKING_TTL_MINUTES);
        pendingBookings.put(syntheticPnr, new PendingBooking(
                outcome.route().flightObject(), outcome.searchReqId(), outcome.hashReqKey(),
                request.passengers(), request.contactEmail(), request.contactPhone(), deadline));

        log.info("[TravelTerminus] Locked price for offer {} as pending booking {} (expires {}); the real "
                        + "book/ticket call is deferred to issueFlightTicket since Travel Terminus has no hold state",
                offer.providerOfferId(), syntheticPnr, deadline);
        return new ProviderBookingConfirmation(getType(), syntheticPnr, deadline, true);
    }

    private void purgeExpiredPendingBookings() {
        LocalDateTime now = LocalDateTime.now();
        pendingBookings.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    @Override
    public FinalTicketConfirmation issueFlightTicket(String pnrCode, PaymentDetails payment) {
        if (config.isMockMode()) {
            return ProviderMockSupport.issueFlightTicket(getType(), pnrCode, 1);
        }
        log.info("[TravelTerminus] issueFlightTicket: starting ticket issuance for pending booking {}", pnrCode);
        PendingBooking pending = pendingBookings.remove(pnrCode);
        if (pending == null) {
            String reason = "No pending booking context for " + pnrCode + " (app restart, or the "
                    + PENDING_BOOKING_TTL_MINUTES + "-minute price lock expired before payment was captured)";
            log.error("[TravelTerminus] {}", reason);
            return new FinalTicketConfirmation(getType(), pnrCode, List.of(), false, reason);
        }
        if (pending.expiresAt().isBefore(LocalDateTime.now())) {
            String reason = "Pending booking " + pnrCode + " expired before payment capture";
            log.warn("[TravelTerminus] {}", reason);
            return new FinalTicketConfirmation(getType(), pnrCode, List.of(), false, reason);
        }
        log.info("[TravelTerminus] issueFlightTicket: pending booking {} found, expires at {}, calling /api/flights/book",
                pnrCode, pending.expiresAt());

        TravelTerminusBookResponse response;
        try {
            TravelTerminusBookRequest bookRequest = toBookRequest(pending);
            TravelTerminusEnvelope<TravelTerminusBookResponse> envelope = withAuth(token -> bookingRestClient.post()
                    .uri("/api/flights/book")
                    .header("access-token", token)
                    .header("accept", "application/json")
                    .header("language", "en")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(bookRequest)
                    .retrieve()
                    .body(new ParameterizedTypeReference<TravelTerminusEnvelope<TravelTerminusBookResponse>>() {
                    }));
            response = envelope != null ? envelope.data() : null;
        } catch (RuntimeException e) {
            String reason = "Book call failed: " + e.getMessage();
            log.error("[TravelTerminus] Book failed for pending booking {}: {}", pnrCode, e.getMessage());
            return new FinalTicketConfirmation(getType(), pnrCode, List.of(), false, reason);
        }
        log.info("[TravelTerminus] issueFlightTicket: /api/flights/book responded for {} (error={}, bookingRefId={})",
                pnrCode, response != null ? response.error() : null, response != null ? response.bookingRefId() : null);

        // Book returns HTTP 200 even for a business failure (no availability / session timeout) -
        // data.error/data.status must be checked explicitly.
        if (response == null || Boolean.TRUE.equals(response.error()) || response.bookingRefId() == null) {
            String reason = response != null && response.orderDetails() != null && !response.orderDetails().isEmpty()
                    ? response.orderDetails().get(0).message() : "no booking reference returned";
            log.error("[TravelTerminus] Book was rejected for pending booking {}: {}", pnrCode, reason);
            return new FinalTicketConfirmation(getType(), pnrCode, List.of(), false, reason);
        }

        String bookingRefId = response.bookingRefId();
        String pnr = response.orderDetails() != null && !response.orderDetails().isEmpty()
                ? response.orderDetails().get(0).pnr() : bookingRefId;
        log.info("[TravelTerminus] issueFlightTicket: booking {} confirmed, fetching ticket numbers for bookingRefId={}",
                pnrCode, bookingRefId);
        List<String> tickets = fetchTicketNumbers(bookingRefId);

        log.info("[TravelTerminus] Booked {} -> bookingRefId={}, pnr={}, tickets={}", pnrCode, bookingRefId, pnr, tickets);
        // bookingRefId (not the synthetic pnrCode) is what BookingService will remember going
        // forward and later pass back into cancelFlightBooking - see its cancellation branch below.
        return new FinalTicketConfirmation(getType(), bookingRefId, tickets, true);
    }

    /** {@code providerConfirmationNumber} here is the {@code bookingRefId} issueFlightTicket
     *  returned (see the comment on that method) - not the original synthetic pnrCode. */
    @Override
    public List<String> checkForIssuedTickets(String providerConfirmationNumber) {
        if (config.isMockMode()) {
            return List.of();
        }
        TravelTerminusOrderDetailsResponse details = fetchOrderDetails(providerConfirmationNumber);
        if (details != null && details.flightTicketNo() != null && !details.flightTicketNo().isBlank()) {
            log.info("[TravelTerminus] checkForIssuedTickets: {} now has ticket(s) {}",
                    providerConfirmationNumber, details.flightTicketNo());
            return splitPipeList(details.flightTicketNo());
        }
        log.info("[TravelTerminus] checkForIssuedTickets: {} still has no ticket numbers (bookingStatus={})",
                providerConfirmationNumber, details != null ? details.bookingStatus() : null);
        return List.of();
    }

    @Override
    public FlightOrderDetail getFlightOrderDetail(String providerConfirmationNumber) {
        if (config.isMockMode()) {
            return null;
        }
        TravelTerminusOrderDetailsResponse details = fetchOrderDetails(providerConfirmationNumber);
        if (details == null) {
            return null;
        }
        List<FlightOrderDetail.Traveler> travelers = details.bookingTravelers() == null ? List.of()
                : details.bookingTravelers().stream().map(this::toTraveler).toList();
        List<FlightOrderDetail.CancellationRule> cancellationRules =
                details.fareRules() == null || details.fareRules().cancellation() == null ? List.of()
                        : details.fareRules().cancellation().stream()
                        .map(rule -> new FlightOrderDetail.CancellationRule(
                                rule.adultCharges() != null ? rule.adultCharges().toPlainString() : null,
                                rule.currency(),
                                Boolean.TRUE.equals(rule.refundable())))
                        .toList();
        return new FlightOrderDetail(details.bookingStatus(), travelers, cancellationRules);
    }

    private FlightOrderDetail.Traveler toTraveler(TravelTerminusOrderDetailsResponse.BookingTraveler traveler) {
        List<FlightOrderDetail.Baggage> baggages = traveler.baggages() == null ? List.of()
                : traveler.baggages().stream()
                .flatMap(group -> (group.bagsData() == null ? List.<TravelTerminusOrderDetailsResponse.BagData>of() : group.bagsData()).stream()
                        .map(bag -> new FlightOrderDetail.Baggage(group.segmentId(), group.departure(), group.arrival(),
                                bag.baggageWeight(), bag.price() != null ? bag.price().toPlainString() : null, bag.currency())))
                .toList();
        List<FlightOrderDetail.Meal> meals = traveler.meals() == null ? List.of()
                : traveler.meals().stream()
                .flatMap(group -> (group.mealsData() == null ? List.<TravelTerminusOrderDetailsResponse.MealData>of() : group.mealsData()).stream()
                        .map(meal -> new FlightOrderDetail.Meal(group.segmentId(), group.departure(), group.arrival(),
                                meal.description(), meal.price() != null ? meal.price().toPlainString() : null, meal.currency())))
                .toList();
        List<FlightOrderDetail.Seat> seats = traveler.seats() == null ? List.of()
                : traveler.seats().stream()
                .flatMap(group -> (group.seatData() == null ? List.<TravelTerminusOrderDetailsResponse.SeatData>of() : group.seatData()).stream()
                        .map(seat -> new FlightOrderDetail.Seat(group.segmentId(), group.departure(), group.arrival(),
                                seat.row(), seat.column(), seat.price() != null ? seat.price().toPlainString() : null, seat.currency())))
                .toList();
        return new FlightOrderDetail.Traveler(traveler.firstName(), traveler.lastName(), traveler.paxType(),
                baggages, meals, seats);
    }

    private TravelTerminusBookRequest toBookRequest(PendingBooking pending) {
        List<TravelTerminusBookRequest.Passenger> passengers = new ArrayList<>();
        for (PassengerInfo passenger : pending.passengers()) {
            passengers.add(toBookPassenger(passenger, pending.contactPhone()));
        }
        // contact.gst is left null: we don't collect GST/business-tax details from travelers today.
        // If a route ever comes back from Revalidate with bookingRequiredValidation.isGSTRequired
        // true, Book will reject it - an accepted v1 gap (flagged for the certification pass).
        TravelTerminusBookRequest.Contact contact = new TravelTerminusBookRequest.Contact(
                pending.contactEmail(), pending.contactPhone(), null, null);
        return new TravelTerminusBookRequest(pending.searchReqId(), pending.hashReqKey(), pending.flightObject(),
                config.getClientIp(), USER_AGENT, passengers, contact);
    }

    private TravelTerminusBookRequest.Passenger toBookPassenger(PassengerInfo passenger, String contactPhone) {
        String[] nameParts = splitName(passenger.fullName());
        TravelTerminusBookRequest.Document document = passenger.passportNumber() != null
                ? new TravelTerminusBookRequest.Document("P", passenger.passportNumber(), null,
                        passenger.passportExpiryDate() != null ? passenger.passportExpiryDate().toString() : null,
                        passenger.passportIssueCountry())
                : null;
        List<TravelTerminusBookRequest.AncillaryRef> baggages = new ArrayList<>();
        List<TravelTerminusBookRequest.AncillaryRef> meals = new ArrayList<>();
        List<TravelTerminusBookRequest.AncillaryRef> seats = new ArrayList<>();
        for (SelectedAncillary selected : passenger.selectedAncillaries()) {
            TravelTerminusBookRequest.AncillaryRef ref = toAncillaryRef(selected.providerToken());
            if (ref == null) {
                continue;
            }
            switch (selected.type()) {
                case BAGGAGE -> baggages.add(ref);
                case MEAL -> meals.add(ref);
                case SEAT -> seats.add(ref);
                case INSURANCE -> { /* GuenTours-only line, never sent to the provider */ }
            }
        }
        return new TravelTerminusBookRequest.Passenger(
                mapPassengerType(passenger.type()),
                DEFAULT_GENDER,
                DEFAULT_TITLE,
                nameParts[0],
                nameParts[1],
                passenger.dateOfBirth() != null ? passenger.dateOfBirth().toString() : null,
                passenger.nationality(),
                passenger.nationality(),
                DEFAULT_CITY,
                contactPhone,
                null,
                document,
                baggages.isEmpty() ? null : baggages,
                meals.isEmpty() ? null : meals,
                seats.isEmpty() ? null : seats
        );
    }

    /** Reconstructs the ancillary reference from a {@link SelectedAncillary#providerToken()} - the
     *  exact JSON this adapter serialized from a Pre Ancillary passenger entry's {@code
     *  flightObject} in {@link #addPaxOptions} - so it can be echoed back into Book's {@code
     *  baggages[]}/{@code meals[]}/{@code seats[]} per the "Connecting to the Book API" doc section. */
    private TravelTerminusBookRequest.AncillaryRef toAncillaryRef(String providerToken) {
        if (providerToken == null) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(providerToken);
            String offerId = node.path("offerId").asText(null);
            String paxRef = node.path("paxRef").asText(null);
            return offerId == null ? null : new TravelTerminusBookRequest.AncillaryRef(offerId, paxRef);
        } catch (IOException e) {
            log.warn("[TravelTerminus] Corrupt ancillary provider token, skipping: {}", e.getMessage());
            return null;
        }
    }

    private static String mapPassengerType(PassengerType type) {
        return switch (type) {
            case ADULT -> "ADT";
            case CHILD -> "CHD";
            case INFANT -> "INF";
        };
    }

    /** Same heuristic as {@code TravelportClient.splitName}: everything before the last space is
     *  the first name, the last token is the last name - our canonical {@code PassengerInfo} only
     *  carries a single {@code fullName} string. */
    private static String[] splitName(String fullName) {
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

    private List<String> fetchTicketNumbers(String bookingRefId) {
        for (int attempt = 1; attempt <= ORDER_DETAILS_POLL_ATTEMPTS; attempt++) {
            TravelTerminusOrderDetailsResponse details = fetchOrderDetails(bookingRefId);
            if (details != null && details.flightTicketNo() != null && !details.flightTicketNo().isBlank()) {
                return splitPipeList(details.flightTicketNo());
            }
            if (details != null && ("Failed".equals(details.bookingStatus()) || "Cancelled".equals(details.bookingStatus()))) {
                break;
            }
            if (attempt < ORDER_DETAILS_POLL_ATTEMPTS) {
                sleepQuietly(ORDER_DETAILS_POLL_DELAY_MS);
            }
        }
        // No ticket numbers yet is not necessarily a failure: Travel Terminus can leave a fresh
        // booking in "Pending" while e-ticketing finishes asynchronously on their side (their own
        // background reconciliation runs every 10 minutes) - the booking itself still stands.
        return List.of();
    }

    private TravelTerminusOrderDetailsResponse fetchOrderDetails(String bookingRefId) {
        try {
            TravelTerminusEnvelope<TravelTerminusOrderDetailsResponse> envelope = withAuth(token -> bookingRestClient.get()
                    .uri("/api/flights/order-details/{bookingRefId}", bookingRefId)
                    .header("access-token", token)
                    .header("accept", "application/json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<TravelTerminusEnvelope<TravelTerminusOrderDetailsResponse>>() {
                    }));
            return envelope != null ? envelope.data() : null;
        } catch (RuntimeException e) {
            log.warn("[TravelTerminus] Order details lookup failed for {}: {}", bookingRefId, e.getMessage());
            return null;
        }
    }

    private static List<String> splitPipeList(String raw) {
        return Arrays.stream(raw.split("\\|")).map(String::strip).filter(s -> !s.isEmpty()).toList();
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ==========================================
    // Cancellation
    // ==========================================

    @Override
    public void cancelFlightBooking(String pnrCode) {
        if (config.isMockMode()) {
            log.info("Mock-cancelled Travel Terminus flight PNR {}", pnrCode);
            return;
        }
        if (pnrCode != null && pnrCode.startsWith(PENDING_BOOKING_PREFIX)) {
            // Never actually booked with Travel Terminus - just a locked price/flightObject that
            // was still (or already expired) in pendingBookings. Nothing to cancel upstream.
            pendingBookings.remove(pnrCode);
            log.info("[TravelTerminus] Discarded pending (never booked) hold {}", pnrCode);
            return;
        }
        try {
            TravelTerminusCancelRequest request = TravelTerminusCancelRequest.fullCancellation(
                    pnrCode, "Customer requested cancellation", config.getClientIp());
            TravelTerminusEnvelope<TravelTerminusCancelResponse> envelope = withAuth(token -> bookingRestClient.post()
                    .uri("/api/flights/cancel-request")
                    .header("access-token", token)
                    .header("accept", "application/json")
                    .header("language", "en")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<TravelTerminusEnvelope<TravelTerminusCancelResponse>>() {
                    }));
            // Travel Terminus queues cancellation requests for manual operator triage - "success"
            // here only means the request was accepted, not that the booking is actually
            // cancelled/refunded yet. There is no synchronous void/refund on this API; poll
            // GET /order-details/{bookingRefId} for bookingStatus=Cancelled if confirmation is needed.
            log.info("[TravelTerminus] Cancellation request queued for {} (status={})",
                    pnrCode, envelope != null && envelope.data() != null ? envelope.data().status() : "unknown");
        } catch (RuntimeException e) {
            throw e instanceof ProviderException pe ? pe
                    : new ProviderException("Travel Terminus cancellation request failed for " + pnrCode + ": " + e.getMessage(), e);
        }
    }

    // ==========================================
    // Hotels / vehicles / properties - not offered by Travel Terminus
    // ==========================================

    @Override
    public List<HotelOffer> searchHotels(HotelSearchCriteria criteria) {
        return List.of();
    }

    @Override
    public HotelPriceVerification verifyHotelPrice(HotelOffer offer, int roomQuantity) {
        throw new UnsupportedOperationException(getType() + " does not support hotel bookings");
    }

    @Override
    public HotelDetail getDetailHotel(HotelOffer offer) {
        throw new UnsupportedOperationException(getType() + " does not support hotel bookings");
    }

    @Override
    public List<RoomOffer> getRoomOffers(HotelOffer offer) {
        throw new UnsupportedOperationException(getType() + " does not support hotel bookings");
    }

    @Override
    public ProviderBookingConfirmation createHotelHold(HotelBookingRequest request) {
        throw new UnsupportedOperationException(getType() + " does not support hotel bookings");
    }

    @Override
    public FinalHotelConfirmation confirmHotelBooking(String hotelBookingRef, PaymentDetails payment) {
        throw new UnsupportedOperationException(getType() + " does not support hotel bookings");
    }

    @Override
    public void cancelHotelBooking(String hotelBookingRef, String providerOfferId, String supplierLocator) {
        throw new UnsupportedOperationException(getType() + " does not support hotel bookings");
    }

    // ==========================================
    // Auth (JWT access-token, 24h TTL, retry-once-on-401)
    // ==========================================

    private String getAccessToken() {
        long now = Instant.now().getEpochSecond();
        String token = cachedAccessToken;
        if (token != null && now < tokenExpiresAtEpochSecond - TOKEN_REFRESH_BUFFER_SECONDS) {
            return token;
        }
        return refreshAccessToken();
    }

    private synchronized String refreshAccessToken() {
        long now = Instant.now().getEpochSecond();
        if (cachedAccessToken != null && now < tokenExpiresAtEpochSecond - TOKEN_REFRESH_BUFFER_SECONDS) {
            return cachedAccessToken; // another thread refreshed while we were waiting for the lock
        }
        TravelTerminusEnvelope<TravelTerminusTokenData> response;
        try {
            response = restClient.post()
                    .uri("/api/auth/generate-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TravelTerminusTokenRequest(config.getApiKey(), config.getApiSecret()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<TravelTerminusEnvelope<TravelTerminusTokenData>>() {
                    });
        } catch (RestClientResponseException e) {
            throw toTravelTerminusException(e);
        }
        if (response == null || response.data() == null || response.data().accessToken() == null) {
            throw new TravelTerminusException("token_error", "Travel Terminus token generation returned no accessToken");
        }
        cachedAccessToken = response.data().accessToken();
        long expiresIn = response.data().expiresIn() != null ? response.data().expiresIn() : 86400L;
        tokenExpiresAtEpochSecond = now + expiresIn;
        return cachedAccessToken;
    }

    /**
     * Runs {@code call} with a cached access token, regenerating and retrying exactly once if
     * Travel Terminus reports the token expired/invalid mid-flight (401 {@code token_expired}/
     * {@code token_invalid}) - matches the vendor's own documented recommendation ("generate a new
     * token immediately and retry the failed request once. Do not retry indefinitely.").
     */
    private <T> T withAuth(Function<String, T> call) {
        String token = getAccessToken();
        try {
            return call.apply(token);
        } catch (RestClientResponseException e) {
            TravelTerminusException mapped = toTravelTerminusException(e);
            if (!isTokenError(mapped.code())) {
                throw mapped;
            }
            cachedAccessToken = null;
            try {
                return call.apply(refreshAccessToken());
            } catch (RestClientResponseException retryEx) {
                throw toTravelTerminusException(retryEx);
            }
        }
    }

    private static boolean isTokenError(String code) {
        return "token_expired".equals(code) || "token_invalid".equals(code);
    }

    private TravelTerminusException toTravelTerminusException(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        if (body != null && !body.isBlank()) {
            try {
                TravelTerminusErrorEnvelope envelope = objectMapper.readValue(body, TravelTerminusErrorEnvelope.class);
                String code = envelope.code() != null ? envelope.code() : "http_" + e.getStatusCode().value();
                String message = envelope.message() != null ? envelope.message() : e.getMessage();
                return new TravelTerminusException(code, message, e);
            } catch (IOException parseEx) {
                log.debug("[TravelTerminus] Failed to parse error envelope: {}", parseEx.getMessage());
            }
        }
        return new TravelTerminusException("http_" + e.getStatusCode().value(), e.getMessage(), e);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("[TravelTerminus] Failed to serialize {}: {}", value, e.getMessage());
            return null;
        }
    }

    // ==========================================
    // Small internal shapes
    // ==========================================

    private record SearchLeg(String departureDate, String origin, String destination) {
    }

    private record SearchPax(String type, int quantity) {
    }

    private record TravelPreferences(String cabinClass, String airTripType) {
    }

    private record RevalidateOutcome(boolean valid, TravelTerminusRoute route, String searchReqId, String hashReqKey) {
        static RevalidateOutcome invalid() {
            return new RevalidateOutcome(false, null, null, null);
        }
    }

    private record PendingBooking(
            JsonNode flightObject,
            String searchReqId,
            String hashReqKey,
            List<PassengerInfo> passengers,
            String contactEmail,
            String contactPhone,
            LocalDateTime expiresAt
    ) {
    }
}
