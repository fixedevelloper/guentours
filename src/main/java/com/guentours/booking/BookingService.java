package com.guentours.booking;

import com.guentours.booking.domain.*;
import com.guentours.booking.event.*;
import com.guentours.booking.service.FlightPricingCalculator;
import com.guentours.booking.web.AncillaryOptionResponse;
import com.guentours.booking.web.AncillaryOptionsRequest;
import com.guentours.booking.web.CheckoutRequest;
import com.guentours.booking.web.MultiCityCheckoutRequest;
import com.guentours.booking.web.TravelerRequest;
import com.guentours.provider.*;
import com.guentours.provider.dto.FlightPriceVerification;
import com.guentours.provider.dto.HotelPriceVerification;
import com.guentours.provider.dto.PropertyPriceVerification;
import com.guentours.provider.dto.VehiclePriceVerification;
import com.guentours.search.OfferCache;
import com.guentours.security.SecurityUtils;
import com.guentours.shared.CommissionPolicy;
import com.guentours.shared.Money;
import com.guentours.shared.exception.BusinessException;
import com.guentours.shared.exception.NotFoundException;
import com.guentours.shared.exception.ProviderException;
import com.guentours.user.domain.User;
import com.guentours.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final OfferCache offerCache;
    private final Map<ProviderType, TravelProviderClient> providerClients;
    private final BookingTrackingService trackingService;
    private final ApplicationEventPublisher events;
    private final CommissionPolicy commissionPolicy;
    private final BigDecimal reservationFeeAmount;
    /** Flat price of the GuenTours travel-insurance ancillary line - never provider-sourced,
     *  always the same amount regardless of offer (see {@link #ancillaryOptions}). */
    private final BigDecimal insuranceFeeAmount;
    /** Self-reference through the Spring proxy, needed so the @Async/@Transactional hold-completion
     *  methods below actually go through AOP when called from within this same class - a direct
     *  `this.foo()` call would bypass both aspects entirely. */
    private final BookingService self;

    public BookingService(BookingRepository bookingRepository, UserService userService, OfferCache offerCache,
                          List<TravelProviderClient> providerClients, BookingTrackingService trackingService,
                          ApplicationEventPublisher events, CommissionPolicy commissionPolicy,
                          @Value("${app.payment.reservation-fee:5000}") BigDecimal reservationFeeAmount,
                          @Value("${app.insurance.flat-fee:2500}") BigDecimal insuranceFeeAmount,
                          @Lazy BookingService self) {
        this.bookingRepository = bookingRepository;
        this.userService = userService;
        this.offerCache = offerCache;
        this.providerClients = providerClients.stream()
                .collect(Collectors.toMap(TravelProviderClient::getType, Function.identity()));
        this.trackingService = trackingService;
        this.events = events;
        this.commissionPolicy = commissionPolicy;
        this.reservationFeeAmount = reservationFeeAmount;
        this.insuranceFeeAmount = insuranceFeeAmount;
        this.self = self;
    }

    /**
     * Quotes priced extras for the "additional options" checkout step: whatever the offer's own
     * provider exposes (baggage/meal/seat - empty for most adapters today, see
     * {@link TravelProviderClient#ancillaryOptions}) plus GuenTours' own flat travel-insurance
     * line, which is never provider-sourced. FLIGHT only; every other offer type returns an empty
     * list. Each option is cached under an opaque id (see {@link OfferCache#cacheAncillaryOption})
     * that the frontend echoes back in {@link TravelerRequest#selectedAncillaryIds} at checkout.
     */
    public List<AncillaryOptionResponse> ancillaryOptions(AncillaryOptionsRequest request) {
        if (request.offerType() != OfferType.FLIGHT) {
            return List.of();
        }
        FlightOffer offer = offerCache.getFlightOffer(request.offerId())
                .orElseThrow(() -> new BusinessException("This flight offer has expired, please search again"));
        TravelProviderClient client = clientFor(offer.providerType());
        List<PassengerInfo> passengers = request.travelers() == null ? List.of() : request.travelers().stream()
                .map(t -> new PassengerInfo(t.fullName(), null, null, t.type(), null, null, null, List.of()))
                .toList();

        List<AncillaryOption> options = new ArrayList<>(client.ancillaryOptions(offer, passengers));
        options.add(new AncillaryOption(AncillaryType.INSURANCE, null, "INSURANCE", "Travel insurance",
                new Money(insuranceFeeAmount, offer.price().currency()), null, null, null));

        return options.stream()
                .map(option -> AncillaryOptionResponse.from(offerCache.cacheAncillaryOption(option), option))
                .toList();
    }

    /**
     * Returns as soon as the booking row exists (status {@code PENDING_HOLD}, everything already
     * known from the cached offer - price, itinerary, travelers), instead of blocking the whole
     * HTTP request on the provider's hold API (verifyPrice + createXHold can each take several
     * seconds, and used to run inline here). The actual provider round trip happens in
     * {@link #completeHold}, off-thread, and pushes its outcome over the existing SSE tracking
     * channel ({@code GET /api/bookings/{id}/track}) once done.
     */
    public Booking checkout(CheckoutRequest request) {
        Booking saved = self.createPendingBooking(request);
        self.completeHold(saved.getId());
        return saved;
    }

    @Transactional
    public Booking createPendingBooking(CheckoutRequest request) {
        User user = userService.findOrCreateForCheckout(request.contactEmail(), request.contactFullName(),
                request.contactPhone());
        List<BookedTraveler> travelers = toBookedTravelers(request.travelers());
        PaymentPlan plan = request.paymentPlan() == null ? PaymentPlan.PAY_NOW : request.paymentPlan();

        Booking booking = switch (request.offerType()) {
            case FLIGHT -> buildPendingFlightBooking(request, user, travelers, plan);
            case HOTEL -> buildPendingHotelBooking(request, user, travelers, plan);
            case CAR_RENTAL -> buildPendingVehicleBooking(request, user, travelers, plan);
            case FURNISHED_RENTAL -> buildPendingPropertyBooking(request, user, travelers, plan);
        };
        booking.assignRetryContext(request.offerId(), request.contactPhone());

        return bookingRepository.save(booking);
    }

    /**
     * The actual provider round trip (price re-verification + hold), run off-thread after
     * {@link #checkout} already returned. Runs in its own transaction, separate from the one that
     * created the booking row, since @Async always executes on a different thread. Never lets an
     * exception escape - a failure here means the booking moves to FAILED with a reason, not a
     * silently-swallowed background error. Reads everything it needs (offer id, contact details,
     * travelers) off the persisted {@link Booking} rather than a request DTO, so the exact same
     * completion logic also serves {@link #retryHold}.
     */
    @Async
    @Transactional
    public void completeHold(String bookingId) {
        Booking booking = getById(bookingId);
        try {
            switch (booking.getOfferType()) {
                case FLIGHT -> completeFlightHold(booking);
                case HOTEL -> completeHotelHold(booking);
                case CAR_RENTAL -> completeVehicleHold(booking);
                case FURNISHED_RENTAL -> completePropertyHold(booking);
            }
            bookingRepository.save(booking);
            events.publishEvent(new BookingCreatedEvent(booking.getId()));
        } catch (OfferExpiredException ex) {
            // Retrying would just resend the exact same now-dead offer id and fail identically -
            // see Booking#canRetryHold - the payer needs to search again, not hit "Retry".
            log.warn("Provider hold failed for booking {} (offer expired, not retryable)", bookingId, ex);
            booking.markFailed(sanitizeFailureReason(ex), false);
            bookingRepository.save(booking);
        } catch (RuntimeException ex) {
            log.warn("Provider hold failed for booking {}", bookingId, ex);
            booking.markFailed(sanitizeFailureReason(ex));
            bookingRepository.save(booking);
        }
        trackingService.publish(bookingId, booking.getStatus());
    }

    public Booking checkoutMultiCity(MultiCityCheckoutRequest request) {
        Booking saved = self.createPendingMultiCityBooking(request);
        self.completeMultiCityHold(saved.getId());
        return saved;
    }

    @Transactional
    public Booking createPendingMultiCityBooking(MultiCityCheckoutRequest request) {
        User user = userService.findOrCreateForCheckout(request.contactEmail(), request.contactFullName(),
                request.contactPhone());
        List<BookedTraveler> travelers = toBookedTravelers(request.travelers());
        PaymentPlan plan = request.paymentPlan() == null ? PaymentPlan.PAY_NOW : request.paymentPlan();

        List<FlightOffer> offers = request.legOfferIds().stream()
                .map(id -> offerCache.getFlightOffer(id)
                        .orElseThrow(() -> new BusinessException("This flight offer has expired, please search again")))
                .toList();
        // Passports must cover the whole itinerary, so check against the last leg's arrival -
        // not just the first leg's departure - see validateFlightTravelers.
        validateFlightTravelers(travelers, offers.getLast().arrivalTime().toLocalDate());
        ProviderType providerType = offers.get(0).providerType();

        List<BookingFlightLeg> itineraryLegs = new ArrayList<>();
        Money total = null;
        for (int i = 0; i < offers.size(); i++) {
            FlightOffer offer = offers.get(i);
            itineraryLegs.add(new BookingFlightLeg(i, offer.airline(), offer.flightNumber(), offer.origin(),
                    offer.destination(), offer.departureTime(), offer.arrivalTime()));
            Money legPriceWithFee = commissionPolicy.addFlightFee(offer.price());
            total = total == null ? legPriceWithFee : total.add(legPriceWithFee);
        }

        String combinedOfferId = offers.stream().map(FlightOffer::providerOfferId).collect(Collectors.joining("|"));
        Booking booking = Booking.forMultiCityFlight(user.getId(), user.getEmail(), providerType, combinedOfferId,
                total, itineraryLegs, travelers);
        booking.applyPaymentPlan(plan, plan == PaymentPlan.PAY_LATER ? reservationFee(total.currency()) : null);
        booking.assignRetryContext(String.join("|", request.legOfferIds()), request.contactPhone());
        return bookingRepository.save(booking);
    }

    @Async
    @Transactional
    public void completeMultiCityHold(String bookingId) {
        Booking booking = getById(bookingId);
        try {
            // OfferExpiredException, not BusinessException: this runs on retry too (see
            // completeFlightHold's comment above for why the exception type matters here).
            List<FlightOffer> offers = legOfferIds(booking).stream()
                    .map(id -> offerCache.getFlightOffer(id)
                            .orElseThrow(() -> new OfferExpiredException("This flight offer has expired, please search again")))
                    .toList();
            ProviderType providerType = offers.getFirst().providerType();
            TravelProviderClient client = clientFor(providerType);
            List<PassengerInfo> passengers = toPassengers(booking.getTravelers());

            List<String> pnrCodes = new ArrayList<>();
            LocalDateTime earliestDeadline = null;
            try {
                for (FlightOffer offer : offers) {
                    FlightPriceVerification verification = client.verifyFlightPrice(offer);
                    if (verification.priceChanged(offer.price()) || !verification.seatsAvailable()) {
                        throw new OfferExpiredException("This flight offer is no longer available at the quoted price, please search again");
                    }
                    ProviderBookingConfirmation hold = client.createFlightHold(
                            new FlightBookingRequest(offer, passengers, booking.getContactEmail(), booking.getContactPhone()));
                    if (!hold.confirmed()) {
                        throw new ProviderException("Unable to hold this flight with " + providerType);
                    }
                    pnrCodes.add(hold.pnrCode());
                    earliestDeadline = earliestDeadline == null || hold.ticketingDeadline().isBefore(earliestDeadline)
                            ? hold.ticketingDeadline() : earliestDeadline;
                }
            } catch (RuntimeException ex) {
                for (String pnr : pnrCodes) {
                    try {
                        client.cancelFlightBooking(pnr);
                    } catch (Exception cleanupEx) {
                        log.warn("Failed to void leg hold {} while rolling back a failed multi-city checkout", pnr, cleanupEx);
                    }
                }
                throw ex;
            }

            booking.markOnHoldMultiLeg(pnrCodes, earliestDeadline);
            bookingRepository.save(booking);
            events.publishEvent(new BookingCreatedEvent(booking.getId()));
        } catch (OfferExpiredException ex) {
            log.warn("Provider hold failed for multi-city booking {} (offer expired, not retryable)", bookingId, ex);
            booking.markFailed(sanitizeFailureReason(ex), false);
            bookingRepository.save(booking);
        } catch (RuntimeException ex) {
            log.warn("Provider hold failed for multi-city booking {}", bookingId, ex);
            booking.markFailed(sanitizeFailureReason(ex));
            bookingRepository.save(booking);
        }
        trackingService.publish(bookingId, booking.getStatus());
    }

    private List<String> legOfferIds(Booking booking) {
        return List.of(booking.getSearchOfferId().split("\\|"));
    }

    /**
     * Only {@link BusinessException} messages (offer expired, price changed, incomplete traveler
     * data, ...) are safe and meaningful to show a guest - they're deliberately authored that way.
     * Anything else is a raw provider/infrastructure failure (I/O errors, timeouts, internal
     * upstream URLs, e.g. a Travelport client exception) that must never reach the client; callers
     * still log the full exception server-side before calling this.
     */
    private String sanitizeFailureReason(Exception ex) {
        if (ex instanceof BusinessException) {
            return ex.getMessage();
        }
        return "Le fournisseur n'a pas pu confirmer cette réservation pour le moment. Vous pouvez réessayer ou recommencer la recherche.";
    }

    /**
     * Resubmits the provider hold for a booking that failed before ever getting a provider PNR/
     * confirmation number. Never allowed once a confirmation exists (rejected by
     * {@link Booking#canRetryHold}), since re-running the hold at that point would risk creating a
     * duplicate reservation with the provider.
     */
    public Booking retryHold(String bookingId) {
        Booking booking = self.markRetrying(bookingId);
        if (!booking.getItineraryLegs().isEmpty()) {
            self.completeMultiCityHold(bookingId);
        } else {
            self.completeHold(bookingId);
        }
        return booking;
    }

    @Transactional
    public Booking markRetrying(String bookingId) {
        Booking booking = getById(bookingId);
        if (!booking.canRetryHold()) {
            throw new BusinessException("Booking " + bookingId + " cannot be retried");
        }
        booking.markRetrying();
        bookingRepository.save(booking);
        trackingService.publish(bookingId, booking.getStatus());
        return booking;
    }

    // --- Fast path: builds a PENDING_HOLD Booking from the cached offer, no provider call ---

    private Booking buildPendingFlightBooking(CheckoutRequest request, User user, List<BookedTraveler> travelers, PaymentPlan plan) {
        FlightOffer offer = offerCache.getFlightOffer(request.offerId())
                .orElseThrow(() -> new BusinessException("This flight offer has expired, please search again"));
        validateFlightTravelers(travelers, offer.departureTime().toLocalDate());
        Money totalOfferPrice = FlightPricingCalculator.multiplyByPayingTravelers(offer.price(), travelers);
        List<BookingExtra> extras = resolveSelectedExtras(request.travelers(), offer.price().currency());
        Money extrasTotal = extras.stream().map(BookingExtra::getPrice)
                .reduce(Money.zero(offer.price().currency()), Money::add);
        Money priceWithFee = commissionPolicy.addFlightFee(totalOfferPrice).add(extrasTotal);
        Booking booking = Booking.forFlight(user.getId(), user.getEmail(), offer.providerType(), offer.providerOfferId(),
                offer.airline(), offer.flightNumber(), offer.origin(), offer.destination(),
                offer.departureTime(), offer.arrivalTime(), offer.cabinClass(), priceWithFee, travelers);
        booking.attachExtras(extras);
        booking.applyPaymentPlan(plan, plan == PaymentPlan.PAY_LATER ? reservationFee(priceWithFee.currency()) : null);
        return booking;
    }

    /**
     * Resolves each traveler's picked ancillary-option ids (see
     * {@link TravelerRequest#selectedAncillaryIds}) against the {@link OfferCache} quote they came
     * from, trusting only the cached price/provider-token - never whatever the client sends.
     * Silently skips an id that's missing/expired (the quote's TTL passed) or priced in a
     * different currency than the booking ({@link Money#add} refuses to mix currencies) rather
     * than failing checkout outright over one stale/inconsistent extra.
     */
    private List<BookingExtra> resolveSelectedExtras(List<TravelerRequest> travelerRequests, String bookingCurrency) {
        List<BookingExtra> extras = new ArrayList<>();
        for (int i = 0; i < travelerRequests.size(); i++) {
            List<String> ids = travelerRequests.get(i).selectedAncillaryIds();
            if (ids == null) {
                continue;
            }
            int travelerIndex = i;
            for (String id : ids) {
                offerCache.getAncillaryOption(id).ifPresentOrElse(option -> {
                    if (!option.price().currency().equals(bookingCurrency)) {
                        log.warn("Skipping ancillary option {} priced in {} for a {} booking (currency mismatch)",
                                id, option.price().currency(), bookingCurrency);
                        return;
                    }
                    extras.add(new BookingExtra(option.type(), travelerIndex, option.segmentId(), option.code(),
                            option.label(), option.price(), option.providerToken()));
                }, () -> log.warn("Selected ancillary option {} not found or expired, skipping", id));
            }
        }
        return extras;
    }

    private Booking buildPendingHotelBooking(CheckoutRequest request, User user, List<BookedTraveler> travelers, PaymentPlan plan) {
        HotelOffer offer = offerCache.getHotelOffer(request.offerId())
                .orElseThrow(() -> new BusinessException("This hotel offer has expired, please search again"));
        Money totalOfferPrice = offer.price().multiply(request.quantityOrDefault());
        Money priceWithFee = commissionPolicy.addHotelFee(totalOfferPrice);
        Booking booking = Booking.forHotel(user.getId(), user.getEmail(), offer.providerType(), offer.providerOfferId(),
                offer.hotelName(), offer.cityCode(), offer.checkIn(), offer.checkOut(), offer.roomType(),
                priceWithFee, travelers, request.quantityOrDefault());
        booking.applyPaymentPlan(plan, plan == PaymentPlan.PAY_LATER ? reservationFee(priceWithFee.currency()) : null);
        return booking;
    }

    private Booking buildPendingVehicleBooking(CheckoutRequest request, User user, List<BookedTraveler> travelers, PaymentPlan plan) {
        VehicleOffer offer = offerCache.getVehicleOffer(request.offerId())
                .orElseThrow(() -> new BusinessException("This vehicle offer has expired, please search again"));
        Money priceWithFee = commissionPolicy.addVehicleFee(offer.totalPrice());
        Booking booking = Booking.forVehicle(user.getId(), user.getEmail(), offer.providerType(), offer.providerOfferId(),
                offer.brand(), offer.model(), offer.category(), offer.transmission(), offer.seats(),
                offer.pickupCity(), offer.dropoffCity(), offer.rentalStart(), offer.pickupTime(),
                offer.rentalEnd(), offer.dropoffTime(), offer.withDriver(), priceWithFee, travelers);
        booking.applyPaymentPlan(plan, plan == PaymentPlan.PAY_LATER ? reservationFee(priceWithFee.currency()) : null);
        return booking;
    }

    private Booking buildPendingPropertyBooking(CheckoutRequest request, User user, List<BookedTraveler> travelers, PaymentPlan plan) {
        PropertyOffer offer = offerCache.getPropertyOffer(request.offerId())
                .orElseThrow(() -> new BusinessException("This property offer has expired, please search again"));
        Money priceWithFee = commissionPolicy.addPropertyFee(offer.totalPrice());
        Booking booking = Booking.forProperty(user.getId(), user.getEmail(), offer.providerType(), offer.providerOfferId(),
                offer.title(), offer.propertyType(), offer.city(), offer.country(), offer.bedrooms(),
                offer.maxGuests(), offer.entirePlace(), offer.checkIn(), offer.checkOut(), priceWithFee, travelers);
        booking.applyPaymentPlan(plan, plan == PaymentPlan.PAY_LATER ? reservationFee(priceWithFee.currency()) : null);
        return booking;
    }

    // --- Async completion: the actual provider round trip, one per offer type ---

    private void completeFlightHold(Booking booking) {
        // OfferExpiredException, not BusinessException: this runs on both the initial hold and a
        // retried one (see completeHold's Javadoc), and BookingService.completeHold specifically
        // catches OfferExpiredException to mark the booking non-retryable - a plain BusinessException
        // here fell through to the generic catch (retryable=true), sending the payer into a "Retry"
        // loop that fails identically every time the offer cache has already evicted this offer.
        FlightOffer offer = offerCache.getFlightOffer(booking.getSearchOfferId())
                .orElseThrow(() -> new OfferExpiredException("This flight offer has expired, please search again"));
        TravelProviderClient client = clientFor(offer.providerType());

        FlightPriceVerification verification = client.verifyFlightPrice(offer);
        if (verification.priceChanged(offer.price()) || !verification.seatsAvailable()) {
            throw new OfferExpiredException("This flight offer is no longer available at the quoted price, please search again");
        }

        List<PassengerInfo> passengers = toPassengers(booking.getTravelers(), booking.getExtras());
        ProviderBookingConfirmation hold = client.createFlightHold(
                new FlightBookingRequest(offer, passengers, booking.getContactEmail(), booking.getContactPhone()));
        if (!hold.confirmed()) {
            throw new ProviderException("Unable to hold this flight with " + offer.providerType());
        }
        booking.markOnHold(hold.pnrCode(), hold.ticketingDeadline());
    }

    private void completeHotelHold(Booking booking) {
        HotelOffer offer = offerCache.getHotelOffer(booking.getSearchOfferId())
                .orElseThrow(() -> new OfferExpiredException("This hotel offer has expired, please search again"));
        TravelProviderClient client = clientFor(offer.providerType());

        HotelPriceVerification verification = client.verifyHotelPrice(offer, booking.getRoomQuantity());

        log.warn(offer.price().toString());
        log.info(verification.toString());

        if (verification.priceChanged(offer.price()) || !verification.available()) {
            throw new OfferExpiredException("This hotel offer is no longer available at the quoted price, please search again");
        }

        List<PassengerInfo> guests = toPassengers(booking.getTravelers());
        ProviderBookingConfirmation hold = client.createHotelHold(
                new HotelBookingRequest(offer, guests, booking.getContactEmail(), booking.getRoomQuantity()));
        if (!hold.confirmed()) {
            throw new ProviderException("Unable to hold this room with " + offer.providerType());
        }
        booking.markOnHold(hold.pnrCode(), hold.ticketingDeadline());
        booking.recordHotelSupplierLocator(hold.supplierLocator());
    }

    private void completeVehicleHold(Booking booking) {
        VehicleOffer offer = offerCache.getVehicleOffer(booking.getSearchOfferId())
                .orElseThrow(() -> new OfferExpiredException("This vehicle offer has expired, please search again"));
        TravelProviderClient client = clientFor(offer.providerType());

        VehiclePriceVerification verification = client.verifyVehiclePrice(offer);
        if (verification.priceChanged(offer.totalPrice()) || !verification.available()) {
            throw new OfferExpiredException("This vehicle offer is no longer available at the quoted price, please search again");
        }

        List<PassengerInfo> drivers = toPassengers(booking.getTravelers());
        ProviderBookingConfirmation hold = client.createVehicleHold(
                new VehicleBookingRequest(offer, drivers, booking.getContactEmail()));
        if (!hold.confirmed()) {
            throw new ProviderException("Unable to hold this vehicle with " + offer.providerType());
        }
        booking.markOnHold(hold.pnrCode(), hold.ticketingDeadline());
    }

    private void completePropertyHold(Booking booking) {
        PropertyOffer offer = offerCache.getPropertyOffer(booking.getSearchOfferId())
                .orElseThrow(() -> new OfferExpiredException("This property offer has expired, please search again"));
        TravelProviderClient client = clientFor(offer.providerType());

        PropertyPriceVerification verification = client.verifyPropertyPrice(offer);
        if (verification.priceChanged(offer.totalPrice()) || !verification.available()) {
            throw new OfferExpiredException("This property offer is no longer available at the quoted price, please search again");
        }

        List<PassengerInfo> guests = toPassengers(booking.getTravelers());
        ProviderBookingConfirmation hold = client.createPropertyHold(
                new PropertyBookingRequest(offer, guests, booking.getContactEmail()));
        if (!hold.confirmed()) {
            throw new ProviderException("Unable to hold this property with " + offer.providerType());
        }
        booking.markOnHold(hold.pnrCode(), hold.ticketingDeadline());
    }

    private Money reservationFee(String currency) {
        return new Money(reservationFeeAmount, currency);
    }

    public Booking getById(String bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
    }

    /**
     * Live baggage/meals/seats/cancellation-policy detail for a confirmed flight booking, straight
     * from the provider (see {@link TravelProviderClient#getFlightOrderDetail}) - not persisted, so
     * this always reflects the provider's current state. Null when the booking isn't a
     * provider-confirmed flight, or when the provider doesn't support this (most adapters - only
     * Travel Terminus does today); callers must fall back to what's already on the Booking itself.
     */
    public FlightOrderDetail getFlightOrderDetail(Booking booking) {
        if (booking.getOfferType() != OfferType.FLIGHT || booking.getProviderConfirmationNumber() == null) {
            return null;
        }
        TravelProviderClient client = clientFor(booking.getProviderType());
        // Multi-city bookings hold one PNR per leg (see Booking#pnrCodes) - query every leg and
        // merge, rather than just the first (booking.getProviderConfirmationNumber()), so a
        // multi-city trip doesn't silently lose the 2nd/3rd leg's baggage/meals/seats here.
        List<FlightOrderDetail> perLeg = booking.pnrCodes().stream()
                .map(client::getFlightOrderDetail)
                .filter(Objects::nonNull)
                .toList();
        if (perLeg.isEmpty()) {
            return null;
        }
        return mergeFlightOrderDetails(perLeg);
    }

    /** Combines one {@link FlightOrderDetail} per leg into one view - same traveler order in every
     *  leg's response (all legs were booked for the same passenger list), so travelers are merged
     *  positionally, concatenating each one's baggage/meals/seats across legs. */
    private FlightOrderDetail mergeFlightOrderDetails(List<FlightOrderDetail> perLeg) {
        if (perLeg.size() == 1) {
            return perLeg.get(0);
        }
        // Distinct statuses across legs joined rather than just taking the first - a multi-city
        // trip where only one leg's ticket is still pending shouldn't quietly report "Confirmed".
        String bookingStatus = perLeg.stream()
                .map(FlightOrderDetail::bookingStatus)
                .filter(Objects::nonNull)
                .distinct()
                .reduce((a, b) -> a + " / " + b)
                .orElse(null);
        int travelerCount = perLeg.stream().mapToInt(d -> d.travelers().size()).max().orElse(0);
        List<FlightOrderDetail.Traveler> mergedTravelers = new ArrayList<>();
        for (int i = 0; i < travelerCount; i++) {
            List<FlightOrderDetail.Baggage> baggages = new ArrayList<>();
            List<FlightOrderDetail.Meal> meals = new ArrayList<>();
            List<FlightOrderDetail.Seat> seats = new ArrayList<>();
            String firstName = null;
            String lastName = null;
            String paxType = null;
            for (FlightOrderDetail leg : perLeg) {
                if (i >= leg.travelers().size()) {
                    continue;
                }
                FlightOrderDetail.Traveler traveler = leg.travelers().get(i);
                firstName = traveler.firstName() != null ? traveler.firstName() : firstName;
                lastName = traveler.lastName() != null ? traveler.lastName() : lastName;
                paxType = traveler.paxType() != null ? traveler.paxType() : paxType;
                baggages.addAll(traveler.baggages());
                meals.addAll(traveler.meals());
                seats.addAll(traveler.seats());
            }
            mergedTravelers.add(new FlightOrderDetail.Traveler(firstName, lastName, paxType, baggages, meals, seats));
        }
        List<FlightOrderDetail.CancellationRule> mergedRules = perLeg.stream()
                .flatMap(d -> d.cancellationRules().stream())
                .toList();
        return new FlightOrderDetail(bookingStatus, mergedTravelers, mergedRules);
    }

    /**
     * Re-checks confirmed flight bookings still missing e-ticket numbers against their provider
     * (see {@link TravelProviderClient#checkForIssuedTickets}) and backfills whichever ones now
     * have them. Lives here rather than in the {@code ticketing} module because talking to a {@link
     * TravelProviderClient} is a booking-module concern (see {@link #clientFor}) - {@code ticketing}
     * isn't allowed to depend on {@code provider} directly (see ModularityTests). Called by {@code
     * ETicketReconciliationJob}, which generates the actual ETicket rows for whatever this returns.
     */
    public List<Booking> reconcileMissingETicketNumbers() {
        List<Booking> pending = bookingRepository.findConfirmedFlightsMissingETickets();
        List<Booking> updated = new ArrayList<>();
        for (Booking booking : pending) {
            if (booking.getProviderConfirmationNumber() == null) {
                continue;
            }
            try {
                TravelProviderClient client = clientFor(booking.getProviderType());
                // Multi-city holds one PNR per leg (see Booking#pnrCodes) - check every leg and
                // aggregate, matching how confirmWithProvider originally collected allTickets
                // across legs, so a multi-city trip isn't only ever reconciled for its first leg.
                List<String> tickets = booking.pnrCodes().stream()
                        .flatMap(pnr -> client.checkForIssuedTickets(pnr).stream())
                        .toList();
                if (!tickets.isEmpty()) {
                    booking.recordETicketNumbers(tickets);
                    bookingRepository.save(booking);
                    updated.add(booking);
                }
            } catch (Exception ex) {
                // One booking's provider hiccup shouldn't stop the rest of the batch - it'll be
                // retried next cycle since it's still CONFIRMED with an empty ticket list.
                log.warn("reconcileMissingETicketNumbers: failed to check booking {}: {}", booking.getId(), ex.getMessage());
            }
        }
        return updated;
    }

    /**
     * Guards guest access to a booking (and anything derived from it: payment, e-tickets, cancel):
     * the caller must be an admin, the marketplace partner whose inventory the booking was made
     * against (partner dashboards list bookings made on their own hotels/flights/vehicles/
     * properties), be authenticated as the account that owns it, or - for the anonymous checkout
     * flow, where no account login is required - supply the same contact email the booking was
     * made with. Reports a 404 rather than 403 on mismatch, so a wrong/guessed id or email never
     * confirms that a booking exists.
     */
    public void verifyGuestAccess(Booking booking, String suppliedEmail) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        String myPartnerId = SecurityUtils.currentPartnerId();
        if (myPartnerId != null && myPartnerId.equals(booking.getPartnerId())) {
            return;
        }
        String authenticatedEmail = SecurityUtils.currentUserEmail();
        String candidate = authenticatedEmail != null ? authenticatedEmail : suppliedEmail;
        if (candidate == null || !candidate.equalsIgnoreCase(booking.getContactEmail())) {
            throw new NotFoundException("Booking not found: " + booking.getId());
        }
    }

    public List<Booking> getForUser(String userId) {
        return bookingRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(Booking::getCreatedAt).reversed())
                .toList();
    }

    public List<Booking> getAll() {
        return bookingRepository.findAll().stream()
                .sorted(Comparator.comparing(Booking::getCreatedAt).reversed())
                .toList();
    }

    public BookingSummary getSummary(String bookingId) {
        return BookingSummary.from(getById(bookingId));
    }

    @Transactional
    public void markDepositPaid(String bookingId) {
        Booking booking = getById(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new BusinessException("Booking " + bookingId + " is not awaiting a reservation fee");
        }
        booking.markDepositPaid();
        bookingRepository.save(booking);
        trackingService.publish(bookingId, BookingStatus.DEPOSIT_PAID);
        events.publishEvent(new ReservationFeePaidEvent(bookingId));
    }

    @Transactional
    public void markPaidAndConfirm(String bookingId, String paymentTransactionReference, String payerReferenceLast4) {
        Booking booking = getById(bookingId);
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT && booking.getStatus() != BookingStatus.DEPOSIT_PAID) {
            throw new BusinessException("Booking " + bookingId + " is not awaiting payment");
        }
        booking.markPaid();
        bookingRepository.save(booking);
        trackingService.publish(bookingId, BookingStatus.PAID);
        events.publishEvent(new BookingPaidEvent(bookingId, paymentTransactionReference, payerReferenceLast4));
    }

    /**
     * ⚠️ CAR_RENTAL/FURNISHED_RENTAL n'ont pas de cycle hold→confirmation finale distinct chez DIRECT
     * (le hold EST la confirmation, cf. DirectClient.createVehicleHold/createPropertyHold) — pas de
     * méthode de finalisation dédiée sur TravelProviderClient pour ces deux types. On marque donc la
     * réservation confirmée directement avec le providerConfirmationNumber déjà posé au hold, sans
     * appel provider supplémentaire. Si un futur adaptateur GDS a un vrai cycle en 2 temps pour ces
     * types, il faudra étendre TravelProviderClient avec les méthodes manquantes et revoir ce court-circuit.
     */
    @Transactional
    public void confirmWithProvider(String bookingId, String paymentTransactionReference, String payerReferenceLast4) {
        Booking booking = getById(bookingId);
        log.info("confirmWithProvider: starting for booking {} (offerType={}, providerType={})",
                bookingId, booking.getOfferType(), booking.getProviderType());
        booking.markConfirming();
        bookingRepository.save(booking);
        trackingService.publish(bookingId, BookingStatus.CONFIRMING);
        log.info("confirmWithProvider: booking {} marked CONFIRMING, proceeding to provider ticket issuance", bookingId);

        try {
            if (booking.getOfferType() == OfferType.CAR_RENTAL || booking.getOfferType() == OfferType.FURNISHED_RENTAL) {
                log.info("confirmWithProvider: booking {} is {} - no separate provider confirmation step, "
                        + "the hold IS the confirmation", bookingId, booking.getOfferType());
                booking.markConfirmed(booking.getProviderConfirmationNumber(), new ArrayList<>());
                bookingRepository.save(booking);
                trackingService.publish(bookingId, BookingStatus.CONFIRMED);
                events.publishEvent(new BookingConfirmedEvent(booking.getId()));
                log.info("confirmWithProvider: booking {} marked CONFIRMED", bookingId);
                return;
            }

            TravelProviderClient client = clientFor(booking.getProviderType());
            PaymentDetails payment = new PaymentDetails(paymentTransactionReference, booking.getPrice(), payerReferenceLast4);

            if (booking.getOfferType() == OfferType.FLIGHT) {
                String primaryConfirmation = null;
                List<String> allTickets = new ArrayList<>();
                List<String> pnrCodes = booking.pnrCodes();
                log.info("confirmWithProvider: booking {} has {} PNR(s) to ticket: {}", bookingId, pnrCodes.size(), pnrCodes);
                for (String pnr : pnrCodes) {
                    log.info("confirmWithProvider: booking {} calling {}.issueFlightTicket for PNR {}",
                            bookingId, booking.getProviderType(), pnr);
                    FinalTicketConfirmation confirmation = client.issueFlightTicket(pnr, payment);
                    log.info("confirmWithProvider: booking {} issueFlightTicket({}) returned issued={}, reason={}",
                            bookingId, pnr, confirmation.issued(), confirmation.reason());
                    if (!confirmation.issued()) {
                        String reason = confirmation.reason() != null
                                ? confirmation.reason()
                                : "provider declined to issue e-tickets";
                        throw new ProviderException(
                                "Provider declined to issue e-tickets for booking " + bookingId + ": " + reason);
                    }
                    if (primaryConfirmation == null) {
                        primaryConfirmation = confirmation.pnrCode();
                    }
                    allTickets.addAll(confirmation.eTicketNumbers());
                }
                booking.markConfirmed(primaryConfirmation, allTickets);
                log.info("confirmWithProvider: booking {} all PNR(s) ticketed, primaryConfirmation={}, tickets={}",
                        bookingId, primaryConfirmation, allTickets);
            } else {
                log.info("confirmWithProvider: booking {} calling {}.confirmHotelBooking for providerConfirmationNumber={}",
                        bookingId, booking.getProviderType(), booking.getProviderConfirmationNumber());
                FinalHotelConfirmation confirmation = client.confirmHotelBooking(booking.getProviderConfirmationNumber(), payment);
                log.info("confirmWithProvider: booking {} confirmHotelBooking returned confirmed={}",
                        bookingId, confirmation.confirmed());
                if (!confirmation.confirmed()) {
                    throw new ProviderException("Provider declined to finalize hotel booking " + bookingId);
                }
                booking.markConfirmed(confirmation.confirmationNumber(), new ArrayList<>());
            }

            bookingRepository.save(booking);
            trackingService.publish(bookingId, BookingStatus.CONFIRMED);

            events.publishEvent(new BookingConfirmedEvent(booking.getId()));
            log.info("confirmWithProvider: booking {} marked CONFIRMED", bookingId);
        } catch (Exception ex) {
            // Not retryable: unlike a hold failure, payment has already been captured here (this
            // runs after markPaidAndConfirm - see BookingConfirmationListener). "Réessayer" would
            // re-run the HOLD flow via markRetrying/completeHold, which doesn't even touch ticket
            // issuance and would risk creating a second provider hold against an already-paid
            // booking. Recovery from a failure at this stage needs manual/ops handling, not a
            // same-flow retry.
            log.error("Provider confirmation failed for booking {} (already paid)", bookingId, ex);
            booking.markFailed(sanitizeFailureReason(ex), false);
            bookingRepository.save(booking);
            trackingService.publish(bookingId, BookingStatus.FAILED);
            events.publishEvent(new BookingFailedEvent(booking.getId()));
        }
    }

    /** Voids every held PNR (one per leg for MULTI_CITY) and marks the booking cancelled. */
    @Transactional
    public Booking cancel(String bookingId) {
        Booking booking = getById(bookingId);
        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.FAILED) {
            throw new BusinessException("Booking " + bookingId + " cannot be cancelled from status " + booking.getStatus());
        }
        if (booking.getStatus() == BookingStatus.PENDING_HOLD) {
            throw new BusinessException("Booking " + bookingId + " is still being confirmed with the provider, please wait a moment before cancelling");
        }

        TravelProviderClient client = clientFor(booking.getProviderType());
        switch (booking.getOfferType()) {
            case FLIGHT -> {
                for (String pnr : booking.pnrCodes()) {
                    client.cancelFlightBooking(pnr);
                }
            }
            case HOTEL -> client.cancelHotelBooking(booking.getProviderConfirmationNumber(),
                    booking.getProviderOfferId(), booking.getHotelSupplierLocator());
            case CAR_RENTAL -> client.cancelVehicleBooking(booking.getProviderConfirmationNumber());
            case FURNISHED_RENTAL -> client.cancelPropertyBooking(booking.getProviderConfirmationNumber());
        }

        booking.markCancelled();
        bookingRepository.save(booking);
        trackingService.publish(bookingId, BookingStatus.CANCELLED);
        return booking;
    }

    @Transactional
    public void cancelExpiredHolds() {
        List<Booking> expired = bookingRepository.findByStatusInAndTicketingDeadlineBefore(
                List.of(BookingStatus.PENDING_PAYMENT, BookingStatus.DEPOSIT_PAID), LocalDateTime.now());
        for (Booking booking : expired) {
            try {
                cancel(booking.getId());
                events.publishEvent(new BookingAutoCancelledEvent(booking.getId(), "HOLD_EXPIRED"));
                log.info("Auto-cancelled expired booking hold {}", booking.getId());
            } catch (Exception ex) {
                log.warn("Failed to auto-cancel expired booking {}: {}", booking.getId(), ex.getMessage());
            }
        }
    }

    public SseEmitter track(String bookingId) {
        getById(bookingId);
        return trackingService.subscribe(bookingId);
    }

    private TravelProviderClient clientFor(ProviderType providerType) {
        TravelProviderClient client = providerClients.get(providerType);
        if (client == null) {
            throw new IllegalStateException("No adapter registered for provider " + providerType);
        }
        return client;
    }

    private List<BookedTraveler> toBookedTravelers(List<TravelerRequest> travelers) {
        return travelers.stream()
                .map(t -> new BookedTraveler(t.fullName(), t.dateOfBirth(), t.passportNumber(), t.type(),
                        t.seatNumber(), t.nationality(), t.passportIssueCountry(), t.passportExpiryDate()))
                .toList();
    }

    private List<PassengerInfo> toPassengers(List<BookedTraveler> travelers) {
        return toPassengers(travelers, List.of());
    }

    /**
     * Same as {@link #toPassengers(List)}, additionally threading each traveler's selected
     * extras (matched by {@link BookingExtra#getTravelerIndex()}, that traveler's position in
     * {@code travelers}) into {@link PassengerInfo#selectedAncillaries()} so the provider adapter
     * can apply them at hold time (see {@code TravelTerminusClient#toBookPassenger}). Only
     * extras with a non-null {@link BookingExtra#getProviderToken()} are provider-bound - a
     * booking-level line like INSURANCE never has one and is simply skipped here.
     */
    private List<PassengerInfo> toPassengers(List<BookedTraveler> travelers, List<BookingExtra> extras) {
        List<PassengerInfo> passengers = new ArrayList<>();
        for (int i = 0; i < travelers.size(); i++) {
            BookedTraveler t = travelers.get(i);
            int travelerIndex = i;
            List<SelectedAncillary> selected = extras.stream()
                    .filter(e -> e.getProviderToken() != null && e.getTravelerIndex() != null
                            && e.getTravelerIndex() == travelerIndex)
                    .map(e -> new SelectedAncillary(e.getType(), e.getProviderToken()))
                    .toList();
            passengers.add(new PassengerInfo(t.getFullName(), t.getDateOfBirth(), t.getPassportNumber(), t.getType(),
                    t.getNationality(), t.getPassportIssueCountry(), t.getPassportExpiryDate(), selected));
        }
        return passengers;
    }

    /**
     * Flight-only: date of birth and nationality are optional on {@link BookedTraveler} because it's
     * shared with hotel/vehicle/property checkouts, which don't need them - but real GDS booking
     * testing confirmed both are actually required for flights (e.g. Travelopro rejects a booking
     * with "PassengerNationality details is required for this airline"). Checked here, before any
     * provider call, so an incomplete submission fails fast with a clear, actionable message instead
     * of a confusing provider-side rejection deep in the booking flow.
     *
     * <p>Passport expiry is still optional (not every route needs a passport at all), but when a
     * traveler did supply one it must cover {@code travelDate} - confirmed by a real Travel Terminus
     * Book rejection ("Passport for passenger1 will be expired before travel date") that otherwise
     * only surfaces at ticket issuance ({@link #confirmWithProvider}), i.e. after payment was already
     * captured. This only catches an outright-expired passport; it doesn't enforce the "valid 6
     * months past travel" rule some destination countries require, since that depends on the
     * destination and isn't modeled here.
     */
    private void validateFlightTravelers(List<BookedTraveler> travelers, LocalDate travelDate) {
        boolean incomplete = travelers.stream()
                .anyMatch(t -> t.getDateOfBirth() == null || t.getNationality() == null || t.getNationality().isBlank());
        if (incomplete) {
            throw new BusinessException("Date of birth and nationality are required for every traveler on a flight booking");
        }
        boolean expiredPassport = travelers.stream()
                .anyMatch(t -> t.getPassportExpiryDate() != null && t.getPassportExpiryDate().isBefore(travelDate));
        if (expiredPassport) {
            throw new BusinessException("One or more travelers' passport will be expired before the travel date - please provide a valid passport expiry date");
        }
    }
}
