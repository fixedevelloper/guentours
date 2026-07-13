package com.guentours.booking;

import com.guentours.provider.ProviderType;
import com.guentours.shared.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OfferType offerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false)
    private ProviderType providerType;

    /**
     * ID of the exact provider offer, as returned by the provider adapter at search time.
     * Some vendors (e.g. Travelopro's FareSourceCode) return an opaque token thousands of
     * characters long, so this is a LOB rather than the default varchar(255). For a
     * MULTI_CITY booking this is every leg's offer id joined with "|".
     */
    @Lob
    @Column(name = "provider_offer_id", nullable = false)
    private String providerOfferId;

    // --- Flight snapshot (populated when offerType == FLIGHT; for MULTI_CITY this mirrors leg 0) ---
    private String airline;
    private String flightNumber;
    private String origin;
    private String destination;
    @Column(name = "departure_time")
    private LocalDateTime departureTime;
    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    /** Populated only for a MULTI_CITY flight booking - one entry per leg, in itinerary order. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_itinerary_legs", joinColumns = @JoinColumn(name = "booking_id"))
    @OrderColumn(name = "leg_position")
    private List<BookingFlightLeg> itineraryLegs = new ArrayList<>();

    /** One PNR per leg for a MULTI_CITY booking; empty for every other booking (use providerConfirmationNumber instead). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_leg_pnr_codes", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "pnr_code")
    @OrderColumn(name = "leg_position")
    private List<String> legPnrCodes = new ArrayList<>();

    // --- Hotel snapshot (populated when offerType == HOTEL) ---
    private String hotelName;
    private String cityCode;
    @Column(name = "check_in")
    private LocalDate checkIn;
    @Column(name = "check_out")
    private LocalDate checkOut;

    /** Cabin class for flights, room type for hotels. */
    @Column(name = "fare_class")
    private String fareClass;

    @Embedded
    private Money price;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_plan", nullable = false)
    private PaymentPlan paymentPlan = PaymentPlan.PAY_NOW;

    /** Only set when paymentPlan == PAY_LATER: the reservation fee due now, balance due before ticketingDeadline. */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "deposit_amount")),
            @AttributeOverride(name = "currency", column = @Column(name = "deposit_currency"))
    })
    private Money depositAmount;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_travelers", joinColumns = @JoinColumn(name = "booking_id"))
    private List<BookedTraveler> travelers = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.PENDING_PAYMENT;

    @Column(name = "provider_confirmation_number")
    private String providerConfirmationNumber;

    /** PNR/reservation hold's deadline as returned by the provider at {@code createFlightHold}/{@code createHotelHold} time. */
    @Column(name = "ticketing_deadline")
    private LocalDateTime ticketingDeadline;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_eticket_numbers", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "eticket_number")
    private List<String> eTicketNumbers = new ArrayList<>();

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    private long version;

    protected Booking() {
        // JPA
    }

    public static Booking forFlight(String userId, String contactEmail, ProviderType providerType,
                                     String providerOfferId, String airline, String flightNumber,
                                     String origin, String destination, LocalDateTime departureTime,
                                     LocalDateTime arrivalTime, String cabinClass, Money price,
                                     List<BookedTraveler> travelers) {
        Booking booking = new Booking();
        booking.userId = userId;
        booking.contactEmail = contactEmail;
        booking.offerType = OfferType.FLIGHT;
        booking.providerType = providerType;
        booking.providerOfferId = providerOfferId;
        booking.airline = airline;
        booking.flightNumber = flightNumber;
        booking.origin = origin;
        booking.destination = destination;
        booking.departureTime = departureTime;
        booking.arrivalTime = arrivalTime;
        booking.fareClass = cabinClass;
        booking.price = price;
        booking.travelers = travelers;
        return booking;
    }

    public static Booking forMultiCityFlight(String userId, String contactEmail, ProviderType providerType,
                                               String combinedProviderOfferId, Money totalPrice,
                                               List<BookingFlightLeg> legs, List<BookedTraveler> travelers) {
        Booking booking = new Booking();
        booking.userId = userId;
        booking.contactEmail = contactEmail;
        booking.offerType = OfferType.FLIGHT;
        booking.providerType = providerType;
        booking.providerOfferId = combinedProviderOfferId;
        booking.itineraryLegs = legs;
        BookingFlightLeg first = legs.get(0);
        booking.airline = first.getAirline();
        booking.flightNumber = first.getFlightNumber();
        booking.origin = first.getOrigin();
        booking.destination = legs.get(legs.size() - 1).getDestination();
        booking.departureTime = first.getDepartureTime();
        booking.arrivalTime = legs.get(legs.size() - 1).getArrivalTime();
        booking.price = totalPrice;
        booking.travelers = travelers;
        return booking;
    }

    public static Booking forHotel(String userId, String contactEmail, ProviderType providerType,
                                    String providerOfferId, String hotelName, String cityCode,
                                    LocalDate checkIn, LocalDate checkOut, String roomType, Money price,
                                    List<BookedTraveler> travelers) {
        Booking booking = new Booking();
        booking.userId = userId;
        booking.contactEmail = contactEmail;
        booking.offerType = OfferType.HOTEL;
        booking.providerType = providerType;
        booking.providerOfferId = providerOfferId;
        booking.hotelName = hotelName;
        booking.cityCode = cityCode;
        booking.checkIn = checkIn;
        booking.checkOut = checkOut;
        booking.fareClass = roomType;
        booking.price = price;
        booking.travelers = travelers;
        return booking;
    }

    public void applyPaymentPlan(PaymentPlan paymentPlan, Money depositAmount) {
        this.paymentPlan = paymentPlan;
        this.depositAmount = depositAmount;
    }

    /** Records the PNR/reservation hold obtained from the provider right after checkout. */
    public void markOnHold(String pnrCode, LocalDateTime ticketingDeadline) {
        this.providerConfirmationNumber = pnrCode;
        this.ticketingDeadline = ticketingDeadline;
        this.updatedAt = Instant.now();
    }

    /** Same as {@link #markOnHold} but for a MULTI_CITY booking holding one PNR per leg. */
    public void markOnHoldMultiLeg(List<String> pnrCodes, LocalDateTime earliestTicketingDeadline) {
        this.legPnrCodes = pnrCodes;
        this.providerConfirmationNumber = pnrCodes.get(0);
        this.ticketingDeadline = earliestTicketingDeadline;
        this.updatedAt = Instant.now();
    }

    /** Returns every PNR held for this booking - one for ordinary bookings, one per leg for MULTI_CITY. */
    public List<String> pnrCodes() {
        return legPnrCodes.isEmpty() ? List.of(providerConfirmationNumber) : legPnrCodes;
    }

    /** Amount the customer must pay right now, given the current status and payment plan. */
    public Money amountDue() {
        if (status == BookingStatus.DEPOSIT_PAID) {
            return price.subtract(depositAmount);
        }
        return paymentPlan == PaymentPlan.PAY_LATER ? depositAmount : price;
    }

    public void markDepositPaid() {
        this.status = BookingStatus.DEPOSIT_PAID;
        this.updatedAt = Instant.now();
    }

    public void markPaid() {
        this.status = BookingStatus.PAID;
        this.updatedAt = Instant.now();
    }

    public void markConfirming() {
        this.status = BookingStatus.CONFIRMING;
        this.updatedAt = Instant.now();
    }

    public void markConfirmed(String providerConfirmationNumber, List<String> eTicketNumbers) {
        this.status = BookingStatus.CONFIRMED;
        this.providerConfirmationNumber = providerConfirmationNumber;
        this.eTicketNumbers = eTicketNumbers;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = BookingStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public void markCancelled() {
        this.status = BookingStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public OfferType getOfferType() {
        return offerType;
    }

    public ProviderType getProviderType() {
        return providerType;
    }

    public String getProviderOfferId() {
        return providerOfferId;
    }

    public String getAirline() {
        return airline;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public List<BookingFlightLeg> getItineraryLegs() {
        return itineraryLegs;
    }

    public List<String> getLegPnrCodes() {
        return legPnrCodes;
    }

    public String getHotelName() {
        return hotelName;
    }

    public String getCityCode() {
        return cityCode;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public String getFareClass() {
        return fareClass;
    }

    public Money getPrice() {
        return price;
    }

    public PaymentPlan getPaymentPlan() {
        return paymentPlan;
    }

    public Money getDepositAmount() {
        return depositAmount;
    }

    public List<BookedTraveler> getTravelers() {
        return travelers;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public String getProviderConfirmationNumber() {
        return providerConfirmationNumber;
    }

    public LocalDateTime getTicketingDeadline() {
        return ticketingDeadline;
    }

    public List<String> getETicketNumbers() {
        return eTicketNumbers;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
