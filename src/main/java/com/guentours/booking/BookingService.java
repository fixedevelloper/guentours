package com.guentours.booking;

import com.guentours.booking.domain.*;
import com.guentours.booking.event.*;
import com.guentours.booking.service.FlightPricingCalculator;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
    /** Self-reference through the Spring proxy, needed so the @Async/@Transactional hold-completion
     *  methods below actually go through AOP when called from within this same class - a direct
     *  `this.foo()` call would bypass both aspects entirely. */
    private final BookingService self;

    public BookingService(BookingRepository bookingRepository, UserService userService, OfferCache offerCache,
                          List<TravelProviderClient> providerClients, BookingTrackingService trackingService,
                          ApplicationEventPublisher events, CommissionPolicy commissionPolicy,
                          @Value("${app.payment.reservation-fee:5000}") BigDecimal reservationFeeAmount,
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
        this.self = self;
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
        validateFlightTravelers(travelers);
        PaymentPlan plan = request.paymentPlan() == null ? PaymentPlan.PAY_NOW : request.paymentPlan();

        List<FlightOffer> offers = request.legOfferIds().stream()
                .map(id -> offerCache.getFlightOffer(id)
                        .orElseThrow(() -> new BusinessException("This flight offer has expired, please search again")))
                .toList();
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
            List<FlightOffer> offers = legOfferIds(booking).stream()
                    .map(id -> offerCache.getFlightOffer(id)
                            .orElseThrow(() -> new BusinessException("This flight offer has expired, please search again")))
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
        validateFlightTravelers(travelers);
        FlightOffer offer = offerCache.getFlightOffer(request.offerId())
                .orElseThrow(() -> new BusinessException("This flight offer has expired, please search again"));
        Money totalOfferPrice = FlightPricingCalculator.multiplyByPayingTravelers(offer.price(), travelers);
        Money priceWithFee = commissionPolicy.addFlightFee(totalOfferPrice);
        Booking booking = Booking.forFlight(user.getId(), user.getEmail(), offer.providerType(), offer.providerOfferId(),
                offer.airline(), offer.flightNumber(), offer.origin(), offer.destination(),
                offer.departureTime(), offer.arrivalTime(), offer.cabinClass(), priceWithFee, travelers);
        booking.applyPaymentPlan(plan, plan == PaymentPlan.PAY_LATER ? reservationFee(priceWithFee.currency()) : null);
        return booking;
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
        FlightOffer offer = offerCache.getFlightOffer(booking.getSearchOfferId())
                .orElseThrow(() -> new BusinessException("This flight offer has expired, please search again"));
        TravelProviderClient client = clientFor(offer.providerType());

        FlightPriceVerification verification = client.verifyFlightPrice(offer);
        if (verification.priceChanged(offer.price()) || !verification.seatsAvailable()) {
            throw new OfferExpiredException("This flight offer is no longer available at the quoted price, please search again");
        }

        List<PassengerInfo> passengers = toPassengers(booking.getTravelers());
        ProviderBookingConfirmation hold = client.createFlightHold(
                new FlightBookingRequest(offer, passengers, booking.getContactEmail(), booking.getContactPhone()));
        if (!hold.confirmed()) {
            throw new ProviderException("Unable to hold this flight with " + offer.providerType());
        }
        booking.markOnHold(hold.pnrCode(), hold.ticketingDeadline());
    }

    private void completeHotelHold(Booking booking) {
        HotelOffer offer = offerCache.getHotelOffer(booking.getSearchOfferId())
                .orElseThrow(() -> new BusinessException("This hotel offer has expired, please search again"));
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
                .orElseThrow(() -> new BusinessException("This vehicle offer has expired, please search again"));
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
                .orElseThrow(() -> new BusinessException("This property offer has expired, please search again"));
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
        booking.markConfirming();
        bookingRepository.save(booking);
        trackingService.publish(bookingId, BookingStatus.CONFIRMING);

        try {
            if (booking.getOfferType() == OfferType.CAR_RENTAL || booking.getOfferType() == OfferType.FURNISHED_RENTAL) {
                booking.markConfirmed(booking.getProviderConfirmationNumber(), new ArrayList<>());
                bookingRepository.save(booking);
                trackingService.publish(bookingId, BookingStatus.CONFIRMED);
                events.publishEvent(new BookingConfirmedEvent(booking.getId()));
                return;
            }

            TravelProviderClient client = clientFor(booking.getProviderType());
            PaymentDetails payment = new PaymentDetails(paymentTransactionReference, booking.getPrice(), payerReferenceLast4);

            if (booking.getOfferType() == OfferType.FLIGHT) {
                String primaryConfirmation = null;
                List<String> allTickets = new ArrayList<>();
                for (String pnr : booking.pnrCodes()) {
                    FinalTicketConfirmation confirmation = client.issueFlightTicket(pnr, payment);
                    if (!confirmation.issued()) {
                        throw new ProviderException("Provider declined to issue e-tickets for booking " + bookingId);
                    }
                    if (primaryConfirmation == null) {
                        primaryConfirmation = confirmation.pnrCode();
                    }
                    allTickets.addAll(confirmation.eTicketNumbers());
                }
                booking.markConfirmed(primaryConfirmation, allTickets);
            } else {
                FinalHotelConfirmation confirmation = client.confirmHotelBooking(booking.getProviderConfirmationNumber(), payment);
                if (!confirmation.confirmed()) {
                    throw new ProviderException("Provider declined to finalize hotel booking " + bookingId);
                }
                booking.markConfirmed(confirmation.confirmationNumber(), new ArrayList<>());
            }

            bookingRepository.save(booking);
            trackingService.publish(bookingId, BookingStatus.CONFIRMED);

            events.publishEvent(new BookingConfirmedEvent(booking.getId()));
        } catch (Exception ex) {
            log.error("Provider confirmation failed for booking {}", bookingId, ex);
            booking.markFailed(sanitizeFailureReason(ex));
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
        return travelers.stream()
                .map(t -> new PassengerInfo(t.getFullName(), t.getDateOfBirth(), t.getPassportNumber(), t.getType(),
                        t.getNationality(), t.getPassportIssueCountry(), t.getPassportExpiryDate()))
                .toList();
    }

    /**
     * Flight-only: date of birth and nationality are optional on {@link BookedTraveler} because it's
     * shared with hotel/vehicle/property checkouts, which don't need them - but real GDS booking
     * testing confirmed both are actually required for flights (e.g. Travelopro rejects a booking
     * with "PassengerNationality details is required for this airline"). Checked here, before any
     * provider call, so an incomplete submission fails fast with a clear, actionable message instead
     * of a confusing provider-side rejection deep in the booking flow.
     */
    private void validateFlightTravelers(List<BookedTraveler> travelers) {
        boolean incomplete = travelers.stream()
                .anyMatch(t -> t.getDateOfBirth() == null || t.getNationality() == null || t.getNationality().isBlank());
        if (incomplete) {
            throw new BusinessException("Date of birth and nationality are required for every traveler on a flight booking");
        }
    }
}
