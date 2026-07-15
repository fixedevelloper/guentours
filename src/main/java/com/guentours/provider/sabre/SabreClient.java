package com.guentours.provider.sabre;

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
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Adapter for Sabre's Dev Studio REST APIs, following Sabre's official AirBookingWorkflow:
 * (1) shop via Bargain Finder Max v5 ({@code POST /v5/offers/shop}), (2) re-check via
 * Revalidate Itinerary ({@code POST /v5/shop/flights/revalidate}), (3/4, optional and not
 * wired yet) Get Seats / Get Ancillaries, (5) create the whole reservation in one call via
 * the Booking Management API ({@code POST /v1/trip/orders/createBooking}, cancelled with
 * {@code /cancelBooking}) - all behind the OAuth2 v3 token exchange handled by
 * {@link SabreTokenProvider}. Ticketing submits an agency-level form of payment via DCCI
 * Payment v2 ({@code POST /v2/dcci/pay}).
 */
@Component
public class SabreClient implements TravelProviderClient {

    private static final Logger log = LoggerFactory.getLogger(SabreClient.class);
    private static final List<String> HOTELS = List.of("Hotel Le Meridien", "Ibis Central", "Sabre Grand Hotel");
    /** Sabre wants full date-times with seconds; LocalDateTime.toString() drops ":00" seconds. */
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final ProviderProperties.Vendor config;
    private final RestClient restClient;
    private final SabreTokenProvider tokenProvider;

    public SabreClient(RestClient.Builder restClientBuilder, ProviderProperties properties) {
        this.config = properties.getSabre();
        ClientHttpRequestFactorySettings timeoutSettings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofMillis(config.getTimeoutMillis()))
                .withReadTimeout(Duration.ofMillis(config.getTimeoutMillis()));
        this.restClient = restClientBuilder
                .baseUrl(config.getBaseUrl().isBlank() ? "https://api.platform.sabre.com" : config.getBaseUrl())
                .requestFactory(ClientHttpRequestFactories.get(timeoutSettings))
                .build();
        this.tokenProvider = new SabreTokenProvider(this.restClient, config);
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
            return config.isMockMode() ? ProviderMockSupport.flights(getType(), criteria, List.of("AF", "DL", "SB"), 1.05)
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
    public FlightPriceVerification verifyFlightPrice(FlightOffer offer) {
        if (config.isMockMode()) {
            return ProviderMockSupport.verifyFlightPrice(offer.providerOfferId());
        }
        return callRevalidateApi(offer);
    }

    @Override
    public HotelPriceVerification verifyHotelPrice(HotelOffer offer) {
        if (config.isMockMode()) {
            return ProviderMockSupport.verifyHotelPrice(offer.providerOfferId());
        }
        return callHotelPriceCheckApi(offer);
    }

    @Override
    public ProviderBookingConfirmation createFlightHold(FlightBookingRequest request) {
        if (config.isMockMode()) {
            return ProviderMockSupport.flightHold(getType());
        }
        return callCreateBookingApi(request);
    }

    @Override
    public ProviderBookingConfirmation createHotelHold(HotelBookingRequest request) {
        if (config.isMockMode()) {
            return ProviderMockSupport.hotelHold(getType());
        }
        return callHotelBookingApi(request);
    }

    @Override
    public FinalTicketConfirmation issueFlightTicket(String pnrCode, PaymentDetails payment) {
        if (config.isMockMode()) {
            return ProviderMockSupport.issueFlightTicket(getType(), pnrCode, 1);
        }
        return callSubmitPaymentApi(pnrCode, payment);
    }

    @Override
    public FinalHotelConfirmation confirmHotelBooking(String hotelBookingRef, PaymentDetails payment) {
        if (config.isMockMode()) {
            return ProviderMockSupport.confirmHotelBooking(getType(), hotelBookingRef);
        }
        // createBooking already created the actual reservation at hold time, and our own
        // PaymentGateway has collected the funds (txn payment.transactionReference()); unlike
        // flights, Sabre hotel orders need no separate ticketing/payment-submission call, so
        // the reservation stands as the confirmation (same reasoning as TravelportClient).
        log.info("Confirmed Sabre hotel reservation {} (charged via txn {})",
                hotelBookingRef, payment.transactionReference());
        return new FinalHotelConfirmation(getType(), hotelBookingRef, hotelBookingRef, true);
    }

    @Override
    public void cancelFlightBooking(String pnrCode) {
        if (config.isMockMode()) {
            log.info("Mock-cancelled Sabre flight PNR {}", pnrCode);
            return;
        }
        restClient.post()
                .uri("/v1/trip/orders/cancelBooking")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                .body(new SabreCancelBookingRequest(pnrCode, true))
                .retrieve()
                .toBodilessEntity();
        log.info("Cancelled Sabre booking {}", pnrCode);
    }

    @Override
    public void cancelHotelBooking(String hotelBookingRef) {
        if (config.isMockMode()) {
            log.info("Mock-cancelled Sabre hotel booking {}", hotelBookingRef);
            return;
        }
        // Each createHotelHold call produces its own dedicated Sabre order (never merged with
        // a flight order), so a full cancelAll of that order - the same Booking Management
        // /cancelBooking call used for flights - is sufficient here.
        restClient.post()
                .uri("/v1/trip/orders/cancelBooking")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                .body(new SabreCancelBookingRequest(hotelBookingRef, true))
                .retrieve()
                .toBodilessEntity();
        log.info("Cancelled Sabre hotel booking {}", hotelBookingRef);
    }

    private List<FlightOffer> callFlightApi(FlightSearchCriteria criteria) {
        SabreOfferSearchRequest request = buildOfferSearchRequest(criteria);

        SabreOfferSearchResponse response = restClient.post()
                .uri("/v5/offers/shop")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                .body(request)
                .retrieve()
                .body(SabreOfferSearchResponse.class);

        var body = response == null ? null : response.groupedItineraryResponse();
        if (body == null || body.itineraryGroups() == null) {
            return List.of();
        }

        Map<Integer, SabreOfferSearchResponse.LegDesc> legsById = body.legDescs() == null ? Map.of()
                : body.legDescs().stream().collect(Collectors.toMap(SabreOfferSearchResponse.LegDesc::id, Function.identity()));
        Map<Integer, SabreOfferSearchResponse.ScheduleDesc> schedulesById = body.scheduleDescs() == null ? Map.of()
                : body.scheduleDescs().stream().collect(Collectors.toMap(SabreOfferSearchResponse.ScheduleDesc::id, Function.identity()));

        return body.itineraryGroups().stream()
                .flatMap(group -> group.itineraries() == null
                        ? List.<FlightOffer>of().stream()
                        : group.itineraries().stream()
                                .map(itinerary -> toFlightOffer(group, itinerary, legsById, schedulesById, criteria)))
                .filter(Objects::nonNull)
                .toList();
    }

    private SabreOfferSearchRequest buildOfferSearchRequest(FlightSearchCriteria criteria) {
        if (criteria.journeyType() == JourneyType.MULTI_CITY) {
            throw new ProviderException("Sabre multi-city search is not supported yet");
        }

        var outbound = new SabreOfferSearchRequest.OriginDestinationInformation(
                criteria.departureDate() + "T00:00:00",
                new SabreOfferSearchRequest.Location(criteria.origin()),
                new SabreOfferSearchRequest.Location(criteria.destination()),
                null);

        List<SabreOfferSearchRequest.OriginDestinationInformation> segments = criteria.journeyType() == JourneyType.ROUND_TRIP
                ? List.of(outbound, new SabreOfferSearchRequest.OriginDestinationInformation(
                        criteria.returnDate() + "T00:00:00",
                        new SabreOfferSearchRequest.Location(criteria.destination()),
                        new SabreOfferSearchRequest.Location(criteria.origin()),
                        null))
                : List.of(outbound);

        List<SabreOfferSearchRequest.PassengerTypeQuantity> passengerTypes = new ArrayList<>();
        passengerTypes.add(new SabreOfferSearchRequest.PassengerTypeQuantity("ADT", Math.max(criteria.adults(), 1)));
        if (criteria.children() > 0) {
            passengerTypes.add(new SabreOfferSearchRequest.PassengerTypeQuantity("CNN", criteria.children()));
        }
        if (criteria.infants() > 0) {
            passengerTypes.add(new SabreOfferSearchRequest.PassengerTypeQuantity("INF", criteria.infants()));
        }
        var travelerAvail = new SabreOfferSearchRequest.AirTravelerAvail(passengerTypes);

        var source = new SabreOfferSearchRequest.Source(
                config.getPseudoCityCode(),
                new SabreOfferSearchRequest.RequestorID("1", "1", new SabreOfferSearchRequest.CompanyName("TN")));

        var rq = new SabreOfferSearchRequest.OTA_AirLowFareSearchRQ(
                "5",
                new SabreOfferSearchRequest.POS(List.of(source)),
                segments,
                new SabreOfferSearchRequest.TravelPreferences(List.of(
                        new SabreOfferSearchRequest.CabinPref(toSabreCabinClass(criteria.cabinClass()), "Preferred"))),
                new SabreOfferSearchRequest.TravelerInfoSummary(
                        List.of(travelerAvail),
                        new SabreOfferSearchRequest.PriceRequestInformation(criteria.currency())),
                new SabreOfferSearchRequest.TPA_Extensions(new SabreOfferSearchRequest.IntelliSellTransaction(
                        new SabreOfferSearchRequest.RequestType("50ITINS"))));

        return new SabreOfferSearchRequest(rq);
    }

    /**
     * Only the outbound leg (the first {@code legs} entry, matching the first
     * {@code groupDescription.legDescriptions} entry) is mapped onto the canonical
     * {@link FlightOffer}'s single origin/destination/departure/arrival - a round-trip
     * itinerary's return leg is not represented in that shape. This mirrors the existing
     * one-way-biased canonical model rather than fixing it here.
     *
     * <p>Schedule times carry only a time-of-day with UTC offset; the flight date comes from
     * the group's leg description. An overnight arrival (landing the day after departure) is
     * not detectable from the verified sample's fields, so arrival is assumed same-day.
     */
    private FlightOffer toFlightOffer(SabreOfferSearchResponse.ItineraryGroup group,
                                       SabreOfferSearchResponse.Itinerary itinerary,
                                       Map<Integer, SabreOfferSearchResponse.LegDesc> legsById,
                                       Map<Integer, SabreOfferSearchResponse.ScheduleDesc> schedulesById,
                                       FlightSearchCriteria criteria) {
        if (itinerary == null || itinerary.legs() == null || itinerary.legs().isEmpty()
                || group.groupDescription() == null || group.groupDescription().legDescriptions() == null
                || group.groupDescription().legDescriptions().isEmpty()) {
            return null;
        }

        SabreOfferSearchResponse.LegDesc outboundLeg = legsById.get(itinerary.legs().get(0).ref());
        if (outboundLeg == null || outboundLeg.schedules() == null || outboundLeg.schedules().isEmpty()) {
            return null;
        }

        List<SabreOfferSearchResponse.ScheduleDesc> schedules = outboundLeg.schedules().stream()
                .map(ref -> schedulesById.get(ref.ref()))
                .filter(Objects::nonNull)
                .toList();
        if (schedules.isEmpty()) {
            return null;
        }

        var first = schedules.get(0);
        var last = schedules.get(schedules.size() - 1);
        if (first.departure() == null || first.departure().time() == null
                || last.arrival() == null || last.arrival().time() == null
                || first.carrier() == null) {
            return null;
        }

        Money price = extractPrice(itinerary, criteria.currency());
        if (price == null) {
            return null;
        }

        LocalDate flightDate = LocalDate.parse(group.groupDescription().legDescriptions().get(0).departureDate());
        LocalDateTime departure = flightDate.atTime(OffsetTime.parse(first.departure().time()).toLocalTime());
        LocalDateTime arrival = flightDate.atTime(OffsetTime.parse(last.arrival().time()).toLocalTime());

        var pricingSegments = firstPassengerSegments(itinerary);
        String cabin = pricingSegments.stream()
                .map(SabreOfferSearchResponse.Segment::cabinCode)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(criteria.cabinClass());
        int seats = pricingSegments.stream()
                .map(SabreOfferSearchResponse.Segment::seatsAvailable)
                .filter(Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(9);

        return new FlightOffer(
                getType(),
                String.valueOf(itinerary.id()),
                first.carrier().marketing(),
                first.carrier().marketing() + first.carrier().marketingFlightNumber(),
                first.departure().airport(),
                last.arrival().airport(),
                departure,
                arrival,
                cabin,
                price,
                seats);
    }

    /** Booked-cabin/seat details of the first priced passenger type (ADT), across all its fare components. */
    private List<SabreOfferSearchResponse.Segment> firstPassengerSegments(SabreOfferSearchResponse.Itinerary itinerary) {
        if (itinerary.pricingInformation() == null || itinerary.pricingInformation().isEmpty()
                || itinerary.pricingInformation().get(0).fare() == null
                || itinerary.pricingInformation().get(0).fare().passengerInfoList() == null) {
            return List.of();
        }
        return itinerary.pricingInformation().get(0).fare().passengerInfoList().stream()
                .map(SabreOfferSearchResponse.PassengerInfoWrapper::passengerInfo)
                .filter(Objects::nonNull)
                .limit(1)
                .flatMap(info -> info.fareComponents() == null ? List.<SabreOfferSearchResponse.FareComponent>of().stream()
                        : info.fareComponents().stream())
                .flatMap(component -> component.segments() == null ? List.<SabreOfferSearchResponse.SegmentWrapper>of().stream()
                        : component.segments().stream())
                .map(SabreOfferSearchResponse.SegmentWrapper::segment)
                .filter(Objects::nonNull)
                .toList();
    }

    private Money extractPrice(SabreOfferSearchResponse.Itinerary itinerary, String fallbackCurrency) {
        if (itinerary.pricingInformation() == null || itinerary.pricingInformation().isEmpty()) {
            return null;
        }
        var totalFare = itinerary.pricingInformation().get(0).fare() == null ? null
                : itinerary.pricingInformation().get(0).fare().totalFare();
        if (totalFare == null) {
            return null;
        }
        String currency = totalFare.currency() != null ? totalFare.currency() : fallbackCurrency;
        if (currency == null) {
            return null;
        }
        return new Money(BigDecimal.valueOf(totalFare.totalPrice()), currency);
    }

    /**
     * Step 2 of Sabre's official AirBookingWorkflow: re-checks price/availability with
     * Revalidate Itinerary before booking, WITHOUT creating a reservation. Revalidation
     * re-prices by itinerary details, so the specific flight is pinned via the OD's
     * {@code TPA_Extensions.Flight} rather than an opaque offer id, and the request type
     * is {@code REVALIDATE} instead of an itinerary count. Reuses
     * {@link SabreOfferSearchResponse} for parsing since the response is the same
     * grouped-itinerary shape as BFM v5.
     *
     * <p>Known approximations pending richer canonical data: only the outbound
     * first/last-segment view kept by {@link FlightOffer} is re-sent (connections in
     * between are not enumerated), and pricing is requested for 1 ADT because passenger
     * counts are not part of the offer.
     */
    private FlightPriceVerification callRevalidateApi(FlightOffer offer) {
        var flight = new SabreOfferSearchRequest.Flight(
                parseFlightNumber(offer),
                DATE_TIME.format(offer.departureTime()),
                DATE_TIME.format(offer.arrivalTime()),
                "A",
                new SabreOfferSearchRequest.Location(offer.origin()),
                new SabreOfferSearchRequest.Location(offer.destination()),
                new SabreOfferSearchRequest.Airline(offer.airline(), offer.airline()));

        var od = new SabreOfferSearchRequest.OriginDestinationInformation(
                DATE_TIME.format(offer.departureTime()),
                new SabreOfferSearchRequest.Location(offer.origin()),
                new SabreOfferSearchRequest.Location(offer.destination()),
                new SabreOfferSearchRequest.OriginDestinationTpaExtensions(List.of(flight)));

        var source = new SabreOfferSearchRequest.Source(
                config.getPseudoCityCode(),
                new SabreOfferSearchRequest.RequestorID("1", "1", new SabreOfferSearchRequest.CompanyName("TN")));

        var rq = new SabreOfferSearchRequest.OTA_AirLowFareSearchRQ(
                "5",
                new SabreOfferSearchRequest.POS(List.of(source)),
                List.of(od),
                null,
                new SabreOfferSearchRequest.TravelerInfoSummary(
                        List.of(new SabreOfferSearchRequest.AirTravelerAvail(List.of(
                                new SabreOfferSearchRequest.PassengerTypeQuantity("ADT", 1)))),
                        new SabreOfferSearchRequest.PriceRequestInformation(offer.price().currency())),
                new SabreOfferSearchRequest.TPA_Extensions(new SabreOfferSearchRequest.IntelliSellTransaction(
                        new SabreOfferSearchRequest.RequestType("REVALIDATE"))));

        SabreOfferSearchResponse response = restClient.post()
                .uri("/v5/shop/flights/revalidate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                .body(new SabreOfferSearchRequest(rq))
                .retrieve()
                .body(SabreOfferSearchResponse.class);

        var body = response == null ? null : response.groupedItineraryResponse();
        var itinerary = body == null || body.itineraryGroups() == null ? null
                : body.itineraryGroups().stream()
                        .flatMap(group -> group.itineraries() == null
                                ? List.<SabreOfferSearchResponse.Itinerary>of().stream() : group.itineraries().stream())
                        .findFirst().orElse(null);

        if (itinerary == null) {
            return new FlightPriceVerification(offer.providerOfferId(), null, false, 0, null);
        }

        Money freshPrice = extractPrice(itinerary, null);
        int seats = firstPassengerSegments(itinerary).stream()
                .map(SabreOfferSearchResponse.Segment::seatsAvailable)
                .filter(Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(9);
        return new FlightPriceVerification(offer.providerOfferId(), freshPrice, seats > 0, seats,
                "Refer to the fare rules returned with this offer");
    }

    private Integer parseFlightNumber(FlightOffer offer) {
        String digits = offer.flightNumber() == null ? "" : offer.flightNumber().replaceAll("\\D", "");
        return digits.isEmpty() ? null : Integer.valueOf(digits);
    }

    /**
     * Step 5 of Sabre's official AirBookingWorkflow: creates the entire reservation
     * (PNR/Order) in a single {@code /createBooking} call, which books the flights,
     * prices and stores the fare, and returns the Sabre confirmation id plus the
     * airline's own confirmation id(s).
     */
    private ProviderBookingConfirmation callCreateBookingApi(FlightBookingRequest request) {
        SabreCreateBookingRequest bookingRequest = new SabreCreateBookingRequest(
                new SabreCreateBookingRequest.FlightOffer(request.offer().providerOfferId()),
                request.passengers().stream().map(this::toSabreTraveler).toList(),
                new SabreCreateBookingRequest.ContactInfo(List.of(request.contactEmail())));

        SabreCreateBookingResponse response = restClient.post()
                .uri("/v1/trip/orders/createBooking")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                .body(bookingRequest)
                .retrieve()
                .body(SabreCreateBookingResponse.class);

        if (response == null || response.confirmationId() == null) {
            String reason = response != null && response.errors() != null && !response.errors().isEmpty()
                    ? response.errors().get(0).description() : "no confirmation id returned";
            throw new ProviderException("Sabre booking creation failed: " + reason);
        }

        // createBooking's known response carries no explicit ticketing time limit; a
        // conservative 24h policy default applies until the real TTL field is confirmed.
        return new ProviderBookingConfirmation(getType(), response.confirmationId(),
                LocalDateTime.now().plusHours(24), true);
    }

    private SabreCreateBookingRequest.Traveler toSabreTraveler(PassengerInfo passenger) {
        String[] nameParts = splitName(passenger.fullName());
        String passengerCode = switch (passenger.type()) {
            case ADULT -> "ADT";
            case CHILD -> "CNN";
            case INFANT -> "INF";
        };
        return new SabreCreateBookingRequest.Traveler(
                nameParts[0],
                nameParts[1],
                passenger.dateOfBirth() != null ? passenger.dateOfBirth().toString() : null,
                passengerCode);
    }

    /**
     * Submits Sabre's DCCI Payment v2 ({@code POST /v2/dcci/pay}) against the held PNR, using
     * an agency/BSP form of payment rather than the customer's card - see
     * {@link SabreSubmitPaymentRequest}'s Javadoc for why. Sabre's NDC/DCCI order model
     * typically auto-issues e-tickets once payment clears, so this doubles as ticketing.
     */
    private FinalTicketConfirmation callSubmitPaymentApi(String pnrCode, PaymentDetails payment) {
        var request = new SabreSubmitPaymentRequest(
                pnrCode,
                new SabreSubmitPaymentRequest.FormOfPayment("AGENCY_BSP", config.getPseudoCityCode()),
                "GuenTours txn " + payment.transactionReference());

        SabreSubmitPaymentResponse response = restClient.post()
                .uri("/v2/dcci/pay")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                .body(request)
                .retrieve()
                .body(SabreSubmitPaymentResponse.class);

        if (response == null || !response.ticketed()) {
            throw new ProviderException("Sabre declined the agency form of payment for PNR " + pnrCode);
        }

        List<String> tickets = response.eTicketNumbers() != null ? response.eTicketNumbers() : List.of();
        return new FinalTicketConfirmation(getType(), pnrCode, tickets, true);
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

    private String toSabreCabinClass(String cabinClass) {
        if (cabinClass == null) {
            return "Y";
        }
        return switch (cabinClass.toUpperCase()) {
            case "BUSINESS" -> "C";
            case "FIRST" -> "F";
            case "PREMIUM_ECONOMY" -> "W";
            default -> "Y";
        };
    }

    /**
     * Searches Sabre's Get Hotel Avail v5 ({@code POST /v5/get/hotelavail}) by IATA city code
     * and maps each property's cheapest rate onto our canonical {@link HotelOffer}. The hotel
     * code, rate plan and adult count are captured in {@code providerContext} so a later
     * price-check/booking can re-fetch a fresh, bookable rate for that exact property.
     */
    private List<HotelOffer> callHotelApi(HotelSearchCriteria criteria) {
        int adults = Math.max(criteria.adults(), 1);
        var request = new SabreHotelAvailRequest(
                new SabreHotelAvailRequest.GetHotelAvailRQ(
                        new SabreHotelAvailRequest.POS(new SabreHotelAvailRequest.Source(config.getPseudoCityCode())),
                        new SabreHotelAvailRequest.SearchCriteria(
                                50, "TotalRate", "ASC", true,
                                new SabreHotelAvailRequest.GeoSearch(
                                        new SabreHotelAvailRequest.GeoRef(30, "MI",
                                                new SabreHotelAvailRequest.RefPoint(criteria.cityCode(), "CODE", "6"))),
                                new SabreHotelAvailRequest.RateInfoRef(
                                        "EUR", "4",
                                        new SabreHotelAvailRequest.StayDateTimeRange(
                                                criteria.checkIn().toString(), criteria.checkOut().toString()),
                                        new SabreHotelAvailRequest.Rooms(List.of(
                                                new SabreHotelAvailRequest.Room(1, adults)))))));

        SabreHotelAvailResponse response = restClient.post()
                .uri("/v5/get/hotelavail")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                .body(request)
                .retrieve()
                .body(SabreHotelAvailResponse.class);

        var body = response == null ? null : response.GetHotelAvailRS();
        if (body == null || body.HotelAvailInfos() == null || body.HotelAvailInfos().HotelAvailInfo() == null) {
            return List.of();
        }

        return body.HotelAvailInfos().HotelAvailInfo().stream()
                .map(info -> toHotelOffer(info, criteria, adults))
                .filter(Objects::nonNull)
                .toList();
    }

    private HotelOffer toHotelOffer(SabreHotelAvailResponse.HotelAvailInfo info, HotelSearchCriteria criteria, int adults) {
        if (info == null || info.HotelInfo() == null || info.HotelInfo().HotelCode() == null
                || info.RateInfos() == null || info.RateInfos().RateInfo() == null
                || info.RateInfos().RateInfo().isEmpty()) {
            return null;
        }
        var cheapest = info.RateInfos().RateInfo().stream()
                .filter(r -> r.ConvertedRateInfo() != null && r.ConvertedRateInfo().AmountAfterTax() != null)
                .min((a, b) -> Double.compare(a.ConvertedRateInfo().AmountAfterTax(), b.ConvertedRateInfo().AmountAfterTax()))
                .orElse(null);
        if (cheapest == null) {
            return null;
        }

        String currency = cheapest.ConvertedRateInfo().CurrencyCode() != null
                ? cheapest.ConvertedRateInfo().CurrencyCode() : "EUR";
        Map<String, String> context = new HashMap<>();
        context.put("hotelCode", info.HotelInfo().HotelCode());
        context.put("adults", String.valueOf(adults));

        return new HotelOffer(
                getType(),
                info.HotelInfo().HotelCode(),
                info.HotelInfo().HotelName(),
                criteria.cityCode(),
                "",
                criteria.checkIn(),
                criteria.checkOut(),
                new Money(BigDecimal.valueOf(cheapest.ConvertedRateInfo().AmountAfterTax()), currency),
                0.0,
                context);
    }

    /**
     * Re-checks a property's rate via Get Hotel Details ({@code POST /v5/get/hoteldetails})
     * then Hotel Price Check ({@code POST /v5/hotel/pricecheck}) - the latter returns the fresh
     * {@code BookingKey} that {@code createBooking} consumes. Keyed by the hotel code captured
     * from search ({@link HotelOffer#context}); when that context is missing (e.g. an offer
     * carrying no context), the check is skipped and the originally quoted price is trusted.
     */
    private HotelPriceVerification callHotelPriceCheckApi(HotelOffer offer) {
        SabreHotelPriceCheckResponse.HotelPriceCheckRS priced = priceCheck(offer);
        if (priced == null) {
            return new HotelPriceVerification(offer.providerOfferId(), null, true, null);
        }
        Money freshPrice = priced.ConvertedRateInfo() != null && priced.ConvertedRateInfo().AmountAfterTax() != null
                ? new Money(BigDecimal.valueOf(priced.ConvertedRateInfo().AmountAfterTax()),
                        priced.ConvertedRateInfo().CurrencyCode() != null
                                ? priced.ConvertedRateInfo().CurrencyCode() : offer.price().currency())
                : null;
        String cancellationPolicy = priced.CancelPolicy() != null ? priced.CancelPolicy().Description() : null;
        return new HotelPriceVerification(offer.providerOfferId(), freshPrice, true, cancellationPolicy);
    }

    /**
     * Fetches a fresh, bookable rate plan for the offer's hotel code (Get Hotel Details) and
     * re-prices it (Hotel Price Check), returning the {@code BookingKey} needed to book.
     * Returns {@code null} when the offer carries no {@code hotelCode} context or the property
     * has no bookable rate plan left.
     */
    private SabreHotelPriceCheckResponse.HotelPriceCheckRS priceCheck(HotelOffer offer) {
        String hotelCode = offer.context("hotelCode");
        if (hotelCode == null) {
            return null;
        }
        int adults = offer.context("adults") != null ? Integer.parseInt(offer.context("adults")) : 1;

        var detailsRequest = new SabreHotelDetailsRequest(
                new SabreHotelDetailsRequest.GetHotelDetailsRQ(
                        new SabreHotelDetailsRequest.POS(new SabreHotelDetailsRequest.Source(config.getPseudoCityCode())),
                        new SabreHotelDetailsRequest.SearchCriteria(
                                new SabreHotelDetailsRequest.HotelRefs(
                                        new SabreHotelDetailsRequest.HotelRef(hotelCode, "GLOBAL")),
                                new SabreHotelDetailsRequest.RateInfoRef(
                                        new SabreHotelDetailsRequest.Rooms(List.of(
                                                new SabreHotelDetailsRequest.Room(1, adults))),
                                        new SabreHotelDetailsRequest.StayDateTimeRange(
                                                offer.checkIn().toString(), offer.checkOut().toString()),
                                        offer.price().currency(), "100"))));

        SabreHotelDetailsResponse detailsResponse = restClient.post()
                .uri("/v5/get/hoteldetails")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                .body(detailsRequest)
                .retrieve()
                .body(SabreHotelDetailsResponse.class);

        var ratePlans = detailsResponse == null || detailsResponse.GetHotelDetailsRS() == null
                || detailsResponse.GetHotelDetailsRS().HotelDetailsInfo() == null
                || detailsResponse.GetHotelDetailsRS().HotelDetailsInfo().HotelRateInfo() == null
                ? null : detailsResponse.GetHotelDetailsRS().HotelDetailsInfo().HotelRateInfo().RatePlans();
        if (ratePlans == null || ratePlans.RatePlan() == null || ratePlans.RatePlan().isEmpty()) {
            return null;
        }
        var cheapestPlan = ratePlans.RatePlan().stream()
                .filter(p -> p.RatePlanCode() != null && p.ConvertedRateInfo() != null
                        && p.ConvertedRateInfo().AmountAfterTax() != null)
                .min((a, b) -> Double.compare(a.ConvertedRateInfo().AmountAfterTax(), b.ConvertedRateInfo().AmountAfterTax()))
                .orElse(null);
        if (cheapestPlan == null) {
            return null;
        }

        var priceCheckRequest = new SabreHotelPriceCheckRequest(
                new SabreHotelPriceCheckRequest.HotelPriceCheckRQ(
                        new SabreHotelPriceCheckRequest.POS(new SabreHotelPriceCheckRequest.Source(config.getPseudoCityCode())),
                        new SabreHotelPriceCheckRequest.HotelRateSelect(hotelCode, cheapestPlan.RatePlanCode(),
                                new SabreHotelPriceCheckRequest.StayDateTimeRange(
                                        offer.checkIn().toString(), offer.checkOut().toString()))));

        SabreHotelPriceCheckResponse priceCheckResponse = restClient.post()
                .uri("/v5/hotel/pricecheck")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                .body(priceCheckRequest)
                .retrieve()
                .body(SabreHotelPriceCheckResponse.class);

        return priceCheckResponse == null ? null : priceCheckResponse.HotelPriceCheckRS();
    }

    /**
     * Books a room via the same Booking Management order endpoint used for flights
     * ({@code POST /v1/trip/orders/createBooking}), sending the fresh {@code BookingKey}
     * obtained from {@link #priceCheck} plus the guest(s). No form of payment is sent here -
     * our own PaymentGateway collects the funds (see {@link #confirmHotelBooking}).
     */
    private ProviderBookingConfirmation callHotelBookingApi(HotelBookingRequest request) {
        SabreHotelPriceCheckResponse.HotelPriceCheckRS priced = priceCheck(request.offer());
        if (priced == null || priced.BookingKey() == null) {
            throw new ProviderException("Sabre hotel booking failed: no bookable rate found for "
                    + request.offer().providerOfferId());
        }

        List<SabreHotelBookingRequest.Guest> guests = request.guests().stream()
                .map(g -> {
                    String[] nameParts = splitName(g.fullName());
                    return new SabreHotelBookingRequest.Guest(nameParts[0], nameParts[1]);
                })
                .toList();

        SabreHotelBookingRequest bookingRequest = new SabreHotelBookingRequest(
                List.of(new SabreHotelBookingRequest.Hotel(priced.BookingKey())),
                guests,
                new SabreHotelBookingRequest.ContactInfo(List.of(request.contactEmail())));

        SabreHotelBookingResponse response = restClient.post()
                .uri("/v1/trip/orders/createBooking")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenProvider.getAccessToken())
                .body(bookingRequest)
                .retrieve()
                .body(SabreHotelBookingResponse.class);

        if (response == null || response.confirmationId() == null) {
            String reason = response != null && response.errors() != null && !response.errors().isEmpty()
                    ? response.errors().get(0).description() : "no confirmation id returned";
            throw new ProviderException("Sabre hotel booking creation failed: " + reason);
        }

        // createBooking's known response carries no explicit hold time limit for hotels; a
        // conservative 24h policy default applies until the real TTL field is confirmed.
        return new ProviderBookingConfirmation(getType(), response.confirmationId(),
                LocalDateTime.now().plusHours(24), true);
    }
}
