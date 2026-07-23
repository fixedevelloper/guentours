package com.guentours.booking;

import com.guentours.provider.FinalHotelConfirmation;
import com.guentours.provider.FinalTicketConfirmation;
import com.guentours.provider.FlightBookingRequest;
import com.guentours.provider.FlightOffer;
import com.guentours.provider.HotelBookingRequest;
import com.guentours.provider.HotelOffer;
import com.guentours.provider.OfferExpiredException;
import com.guentours.provider.PassengerInfo;
import com.guentours.provider.PaymentDetails;
import com.guentours.provider.ProviderBookingConfirmation;
import com.guentours.provider.ProviderType;
import com.guentours.provider.TravelProviderClient;
import com.guentours.provider.dto.FlightPriceVerification;
import com.guentours.provider.dto.HotelPriceVerification;
import com.guentours.search.OfferCache;
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

    public BookingService(BookingRepository bookingRepository, UserService userService, OfferCache offerCache,
                           List<TravelProviderClient> providerClients, BookingTrackingService trackingService,
                           ApplicationEventPublisher events, CommissionPolicy commissionPolicy,
                           @Value("${app.payment.reservation-fee:5000}") BigDecimal reservationFeeAmount) {
        this.bookingRepository = bookingRepository;
        this.userService = userService;
        this.offerCache = offerCache;
        this.providerClients = providerClients.stream()
                .collect(Collectors.toMap(TravelProviderClient::getType, Function.identity()));
        this.trackingService = trackingService;
        this.events = events;
        this.commissionPolicy = commissionPolicy;
        this.reservationFeeAmount = reservationFeeAmount;
    }

    /**
     * Re-validates the offer with the provider and places a PNR/reservation hold on it
     * before ever charging the customer - so a stale or oversold offer fails fast at
     * checkout instead of surfacing as a payment that can't actually be honored.
     */
    @Transactional
    public Booking checkout(CheckoutRequest request) {
        User user = userService.findOrCreateForCheckout(request.contactEmail(), request.contactFullName(),
                request.contactPhone());

        List<BookedTraveler> travelers = toBookedTravelers(request.travelers());
        PaymentPlan plan = request.paymentPlan() == null ? PaymentPlan.PAY_NOW : request.paymentPlan();

        Booking booking = switch (request.offerType()) {
            case FLIGHT -> checkoutFlight(request, user, travelers, plan);
            case HOTEL -> checkoutHotel(request, user, travelers, plan);
            case CAR_RENTAL -> null;
            case FURNISHED_RENTAL -> null;
        };

        Booking saved = bookingRepository.save(booking);
        events.publishEvent(new BookingCreatedEvent(saved.getId()));
        return saved;
    }

    /**
     * Books every leg of a MULTI_CITY itinerary with the same provider (as returned by one
     * {@code MultiCityItinerary}) into a single booking. If any leg's price/availability
     * check or hold fails, every already-placed hold for this itinerary is voided.
     */
    @Transactional
    public Booking checkoutMultiCity(MultiCityCheckoutRequest request) {
        User user = userService.findOrCreateForCheckout(request.contactEmail(), request.contactFullName(),
                request.contactPhone());
        List<BookedTraveler> travelers = toBookedTravelers(request.travelers());
        PaymentPlan plan = request.paymentPlan() == null ? PaymentPlan.PAY_NOW : request.paymentPlan();
        List<PassengerInfo> passengers = toPassengers(travelers);

        List<FlightOffer> offers = request.legOfferIds().stream()
                .map(id -> offerCache.getFlightOffer(id)
                        .orElseThrow(() -> new BusinessException("This flight offer has expired, please search again")))
                .toList();

        ProviderType providerType = offers.get(0).providerType();
        TravelProviderClient client = clientFor(providerType);

        List<String> pnrCodes = new ArrayList<>();
        List<BookingFlightLeg> itineraryLegs = new ArrayList<>();
        Money total = null;
        LocalDateTime earliestDeadline = null;

        try {
            for (int i = 0; i < offers.size(); i++) {
                FlightOffer offer = offers.get(i);
                FlightPriceVerification verification = client.verifyFlightPrice(offer);
                if (verification.priceChanged(offer.price()) || !verification.seatsAvailable()) {
                    throw new OfferExpiredException(
                            "This flight offer is no longer available at the quoted price, please search again");
                }
                ProviderBookingConfirmation hold = client.createFlightHold(
                        new FlightBookingRequest(offer, passengers, request.contactEmail()));
                if (!hold.confirmed()) {
                    throw new ProviderException("Unable to hold this flight with " + providerType);
                }
                pnrCodes.add(hold.pnrCode());
                itineraryLegs.add(new BookingFlightLeg(i, offer.airline(), offer.flightNumber(), offer.origin(),
                        offer.destination(), offer.departureTime(), offer.arrivalTime()));
                Money legPriceWithFee = commissionPolicy.addFlightFee(offer.price());
                total = total == null ? legPriceWithFee : total.add(legPriceWithFee);
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

        String combinedOfferId = offers.stream().map(FlightOffer::providerOfferId).collect(Collectors.joining("|"));
        Booking booking = Booking.forMultiCityFlight(user.getId(), user.getEmail(), providerType, combinedOfferId,
                total, itineraryLegs, travelers);
        booking.applyPaymentPlan(plan, plan == PaymentPlan.PAY_LATER ? reservationFee(total.currency()) : null);
        booking.markOnHoldMultiLeg(pnrCodes, earliestDeadline);
        Booking saved = bookingRepository.save(booking);
        events.publishEvent(new BookingCreatedEvent(saved.getId()));
        return saved;
    }

    private Booking checkoutFlight(CheckoutRequest request, User user, List<BookedTraveler> travelers, PaymentPlan plan) {
        FlightOffer offer = offerCache.getFlightOffer(request.offerId())
                .orElseThrow(() -> new BusinessException("This flight offer has expired, please search again"));
        TravelProviderClient client = clientFor(offer.providerType());

        FlightPriceVerification verification = client.verifyFlightPrice(offer);
        if (verification.priceChanged(offer.price()) || !verification.seatsAvailable()) {
            throw new OfferExpiredException("This flight offer is no longer available at the quoted price, please search again");
        }

        List<PassengerInfo> passengers = toPassengers(travelers);
        ProviderBookingConfirmation hold = client.createFlightHold(
                new FlightBookingRequest(offer, passengers, request.contactEmail()));
        if (!hold.confirmed()) {
            throw new ProviderException("Unable to hold this flight with " + offer.providerType());
        }

        Money priceWithFee = commissionPolicy.addFlightFee(offer.price());
        Booking booking = Booking.forFlight(user.getId(), user.getEmail(), offer.providerType(), offer.providerOfferId(),
                offer.airline(), offer.flightNumber(), offer.origin(), offer.destination(),
                offer.departureTime(), offer.arrivalTime(), offer.cabinClass(), priceWithFee, travelers);
        booking.applyPaymentPlan(plan, plan == PaymentPlan.PAY_LATER ? reservationFee(priceWithFee.currency()) : null);
        booking.markOnHold(hold.pnrCode(), hold.ticketingDeadline());
        return booking;
    }

    private Booking checkoutHotel(CheckoutRequest request, User user, List<BookedTraveler> travelers, PaymentPlan plan) {
        HotelOffer offer = offerCache.getHotelOffer(request.offerId())
                .orElseThrow(() -> new BusinessException("This hotel offer has expired, please search again"));
        TravelProviderClient client = clientFor(offer.providerType());

        HotelPriceVerification verification = client.verifyHotelPrice(offer);
        if (verification.priceChanged(offer.price()) || !verification.available()) {
            throw new OfferExpiredException("This hotel offer is no longer available at the quoted price, please search again");
        }

        List<PassengerInfo> guests = toPassengers(travelers);
        ProviderBookingConfirmation hold = client.createHotelHold(
                new HotelBookingRequest(offer, guests, request.contactEmail()));
        if (!hold.confirmed()) {
            throw new ProviderException("Unable to hold this room with " + offer.providerType());
        }

        Money priceWithFee = commissionPolicy.addHotelFee(offer.price());
        Booking booking = Booking.forHotel(user.getId(), user.getEmail(), offer.providerType(), offer.providerOfferId(),
                offer.hotelName(), offer.cityCode(), offer.checkIn(), offer.checkOut(), offer.roomType(),
                priceWithFee, travelers);
        booking.applyPaymentPlan(plan, plan == PaymentPlan.PAY_LATER ? reservationFee(priceWithFee.currency()) : null);
        booking.markOnHold(hold.pnrCode(), hold.ticketingDeadline());
        return booking;
    }

    /** The fixed, non-refundable reservation fee due up front for a PAY_LATER booking (never deducted from the price). */
    private Money reservationFee(String currency) {
        return new Money(reservationFeeAmount, currency);
    }

    public Booking getById(String bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
    }

    /** Every booking made by the given account, most recent first - backs the customer dashboard. */
    public List<Booking> getForUser(String userId) {
        return bookingRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(Booking::getCreatedAt).reversed())
                .toList();
    }

    /** Every booking across every account, most recent first - backs the admin dashboard. */
    public List<Booking> getAll() {
        return bookingRepository.findAll().stream()
                .sorted(Comparator.comparing(Booking::getCreatedAt).reversed())
                .toList();
    }

    public BookingSummary getSummary(String bookingId) {
        return BookingSummary.from(getById(bookingId));
    }

    /**
     * Called by the payment module right after a customer pays only the non-refundable reservation
     * fee of a PAY_LATER booking. Unlike {@link #markPaidAndConfirm}, this does NOT trigger provider
     * ticket issuance - the hold stays open until the full price is paid (or it lapses and gets
     * auto-cancelled by {@link #cancelExpiredHolds()}). Fires {@link ReservationFeePaidEvent} so the
     * fee is recorded as reservation commission.
     */
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

    /**
     * Called by the payment module right after a charge succeeds for the full price (PAY_NOW)
     * or the remaining balance (PAY_LATER, once the deposit was already paid). Marks the
     * booking PAID immediately (fast path for the payment response) and publishes
     * {@link BookingPaidEvent}; {@link BookingConfirmationListener} picks that up after commit
     * and drives provider ticket issuance - which can be slow - on a background thread, so the
     * client follows progress via {@code GET /api/bookings/{id}/track} instead of blocking on it here.
     */
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

    @Transactional
    void confirmWithProvider(String bookingId, String paymentTransactionReference, String payerReferenceLast4) {
        Booking booking = getById(bookingId);
        booking.markConfirming();
        bookingRepository.save(booking);
        trackingService.publish(bookingId, BookingStatus.CONFIRMING);

        try {
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
            booking.markFailed(ex.getMessage());
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

        TravelProviderClient client = clientFor(booking.getProviderType());
        if (booking.getOfferType() == OfferType.FLIGHT) {
            for (String pnr : booking.pnrCodes()) {
                client.cancelFlightBooking(pnr);
            }
        } else {
            client.cancelHotelBooking(booking.getProviderConfirmationNumber());
        }

        booking.markCancelled();
        bookingRepository.save(booking);
        trackingService.publish(bookingId, BookingStatus.CANCELLED);
        return booking;
    }

    /** Cancels every unpaid/deposit-only hold whose provider ticketing deadline has lapsed. */
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

    SseEmitter track(String bookingId) {
        getById(bookingId); // 404s early if the booking doesn't exist
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
                .map(t -> new BookedTraveler(t.fullName(), t.dateOfBirth(), t.passportNumber(), t.type(), t.seatNumber()))
                .toList();
    }

    private List<PassengerInfo> toPassengers(List<BookedTraveler> travelers) {
        return travelers.stream()
                .map(t -> new PassengerInfo(t.getFullName(), t.getDateOfBirth(), t.getPassportNumber(), t.getType()))
                .toList();
    }
}
