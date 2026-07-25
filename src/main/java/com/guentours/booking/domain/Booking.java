package com.guentours.booking.domain;

import com.guentours.provider.ProviderType;
import com.guentours.shared.Money;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings")
@Getter
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "offer_type", nullable = false, length = 30)
    private OfferType offerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 30)
    private ProviderType providerType;

    /** Référence externe vers un partenaire d'inventaire direct (module partners). */
    @Column(name = "partner_id", length = 36)
    private String partnerId;

    /** Référence externe vers le revendeur/apporteur d'affaires (module reseller). */
    @Column(name = "reseller_id", length = 36)
    private String resellerId;

    /**
     * Identifiant unique de l'offre fournisseur au moment de la recherche.
     * Utilise un LOB pour supporter les jetons longs (ex: FareSourceCode de Travelopro).
     */
    @Lob
    @Column(name = "provider_offer_id", nullable = false)
    private String providerOfferId;

    // --- Snapshot Vol (Populé si offerType == FLIGHT) ---
    private String airline;

    @Column(name = "flight_number")
    private String flightNumber;

    private String origin;
    private String destination;

    @Column(name = "departure_time")
    private LocalDateTime departureTime;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    /** Tronçons d'un itinéraire multi-destinations (MULTI_CITY). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_itinerary_legs", joinColumns = @JoinColumn(name = "booking_id"))
    @OrderColumn(name = "leg_position")
    private List<BookingFlightLeg> itineraryLegs = new ArrayList<>();

    /** Codes PNR par tronçon pour un vol multi-destinations. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_leg_pnr_codes", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "pnr_code")
    @OrderColumn(name = "leg_position")
    private List<String> legPnrCodes = new ArrayList<>();

    // --- Snapshot Hôtel (Populé si offerType == HOTEL) ---
    @Column(name = "hotel_name")
    private String hotelName;

    @Column(name = "city_code")
    private String cityCode;

    @Column(name = "check_in")
    private LocalDate checkIn;

    @Column(name = "check_out")
    private LocalDate checkOut;

    /** Classe de cabine (Vol) ou type de chambre (Hôtel). */
    @Column(name = "fare_class")
    private String fareClass;

    @Embedded
    private Money price;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_plan", nullable = false, length = 20)
    private PaymentPlan paymentPlan = PaymentPlan.PAY_NOW;

    /** Frais d'acompte/réservation non remboursables pour l'option PAY_LATER. */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "amount", column = @Column(name = "deposit_amount")),
            @AttributeOverride(name = "currency", column = @Column(name = "deposit_currency"))
    })
    private Money reservationFee;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_travelers", joinColumns = @JoinColumn(name = "booking_id"))
    private List<BookedTraveler> travelers = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingStatus status = BookingStatus.PENDING_PAYMENT;

    @Column(name = "provider_confirmation_number")
    private String providerConfirmationNumber;

    @Column(name = "ticketing_deadline")
    private LocalDateTime ticketingDeadline;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_eticket_numbers", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "eticket_number")
    private List<String> eTicketNumbers = new ArrayList<>();

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    private long version;

    protected Booking() {
        // Constructeur JPA
    }

    // =========================================================================
    // Factory Methods (Création de réservations)
    // =========================================================================

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

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // =========================================================================
    // Logique Métier & Transitions d'état
    // =========================================================================

    /** Associe cette réservation à un revendeur via son ID. */
    public void assignReseller(String resellerId) {
        this.resellerId = resellerId;
    }

    /** Associe cette réservation à un partenaire d'inventaire direct. */
    public void assignPartner(String partnerId) {
        this.partnerId = partnerId;
    }

    public void applyPaymentPlan(PaymentPlan paymentPlan, Money reservationFee) {
        this.paymentPlan = paymentPlan;
        this.reservationFee = reservationFee;
    }

    public void markOnHold(String pnrCode, LocalDateTime ticketingDeadline) {
        this.providerConfirmationNumber = pnrCode;
        this.ticketingDeadline = ticketingDeadline;
    }

    public void markOnHoldMultiLeg(List<String> pnrCodes, LocalDateTime earliestTicketingDeadline) {
        this.legPnrCodes = pnrCodes;
        if (!pnrCodes.isEmpty()) {
            this.providerConfirmationNumber = pnrCodes.get(0);
        }
        this.ticketingDeadline = earliestTicketingDeadline;
    }

    /** Renvoie la liste de tous les codes PNR associés à la réservation. */
    public List<String> pnrCodes() {
        return legPnrCodes.isEmpty() ? List.of(providerConfirmationNumber) : legPnrCodes;
    }

    /**
     * Montant exigible immédiatement selon l'état actuel et l'option de paiement.
     */
    public Money amountDue() {
        if (status == BookingStatus.DEPOSIT_PAID) {
            return price;
        }
        return paymentPlan == PaymentPlan.PAY_LATER ? reservationFee : price;
    }

    public void markDepositPaid() {
        this.status = BookingStatus.DEPOSIT_PAID;
    }

    public void markPaid() {
        this.status = BookingStatus.PAID;
    }

    public void markConfirming() {
        this.status = BookingStatus.CONFIRMING;
    }

    public void markConfirmed(String providerConfirmationNumber, List<String> eTicketNumbers) {
        this.status = BookingStatus.CONFIRMED;
        this.providerConfirmationNumber = providerConfirmationNumber;
        this.eTicketNumbers = eTicketNumbers;
    }

    public void markFailed(String reason) {
        this.status = BookingStatus.FAILED;
        this.failureReason = reason;
    }

    public void markCancelled() {
        this.status = BookingStatus.CANCELLED;
    }
}