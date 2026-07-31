package com.guentours.booking.domain;

import com.guentours.provider.ProviderType;
import com.guentours.shared.Money;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    @Column(name = "contact_phone")
    private String contactPhone;

    /**
     * The search/offer-cache id(s) used to create this booking - pipe-joined for a multi-city
     * itinerary, one per leg in order. Kept around solely so a failed provider hold can be
     * retried without asking the guest to fill in their details again; {@code null} for
     * bookings created before this field existed.
     */
    @Column(name = "search_offer_id")
    private String searchOfferId;

    @Enumerated(EnumType.STRING)
    @Column(name = "offer_type", nullable = false, length = 30)
    private OfferType offerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 30)
    private ProviderType providerType;

    @Column(name = "partner_id", length = 36)
    private String partnerId;

    @Column(name = "reseller_id", length = 36)
    private String resellerId;

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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_itinerary_legs", joinColumns = @JoinColumn(name = "booking_id"))
    @OrderColumn(name = "leg_position")
    private List<BookingFlightLeg> itineraryLegs = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_leg_pnr_codes", joinColumns = @JoinColumn(name = "booking_id"))
    @Column(name = "pnr_code")
    @OrderColumn(name = "leg_position")
    private List<String> legPnrCodes = new ArrayList<>();

    // --- Snapshot Hôtel (Populé si offerType == HOTEL) ; cityCode/checkIn/checkOut réutilisés pour FURNISHED_RENTAL ---
    @Column(name = "hotel_name")
    private String hotelName;

    @Column(name = "city_code")
    private String cityCode;

    @Column(name = "check_in")
    private LocalDate checkIn;

    @Column(name = "check_out")
    private LocalDate checkOut;

    /** Classe de cabine (Vol), type de chambre (Hôtel), ou catégorie/type non repris ici pour véhicule/logement. */
    @Column(name = "fare_class")
    private String fareClass;

    // --- Snapshot Véhicule (Populé si offerType == CAR_RENTAL) ---
    @Column(name = "vehicle_brand")
    private String vehicleBrand;

    @Column(name = "vehicle_model")
    private String vehicleModel;

    @Column(name = "vehicle_category")
    private String vehicleCategory;

    @Column(name = "vehicle_transmission")
    private String vehicleTransmission;

    @Column(name = "vehicle_seats")
    private Integer vehicleSeats;

    @Column(name = "pickup_city")
    private String pickupCity;

    @Column(name = "dropoff_city")
    private String dropoffCity;

    @Column(name = "rental_start")
    private LocalDate rentalStart;

    @Column(name = "pickup_time")
    private LocalTime pickupTime;

    @Column(name = "rental_end")
    private LocalDate rentalEnd;

    @Column(name = "dropoff_time")
    private LocalTime dropoffTime;

    @Column(name = "with_driver")
    private Boolean withDriver;

    // --- Snapshot Logement meublé (Populé si offerType == FURNISHED_RENTAL) ---
    @Column(name = "property_title")
    private String propertyTitle;

    @Column(name = "property_type")
    private String propertyType;

    private String country;

    private Integer bedrooms;

    @Column(name = "max_guests")
    private Integer maxGuests;

    @Column(name = "entire_place")
    private Boolean entirePlace;

    @Embedded
    private Money price;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_plan", nullable = false, length = 20)
    private PaymentPlan paymentPlan = PaymentPlan.PAY_NOW;

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
    private BookingStatus status = BookingStatus.PENDING_HOLD;

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

    public static Booking forVehicle(String userId, String contactEmail, ProviderType providerType,
                                     String providerOfferId, String brand, String model, String category,
                                     String transmission, int seats, String pickupCity, String dropoffCity,
                                     LocalDate rentalStart, LocalTime pickupTime, LocalDate rentalEnd,
                                     LocalTime dropoffTime, boolean withDriver, Money price,
                                     List<BookedTraveler> drivers) {
        Booking booking = new Booking();
        booking.userId = userId;
        booking.contactEmail = contactEmail;
        booking.offerType = OfferType.CAR_RENTAL;
        booking.providerType = providerType;
        booking.providerOfferId = providerOfferId;
        booking.vehicleBrand = brand;
        booking.vehicleModel = model;
        booking.vehicleCategory = category;
        booking.vehicleTransmission = transmission;
        booking.vehicleSeats = seats;
        booking.pickupCity = pickupCity;
        booking.dropoffCity = dropoffCity;
        booking.rentalStart = rentalStart;
        booking.pickupTime = pickupTime;
        booking.rentalEnd = rentalEnd;
        booking.dropoffTime = dropoffTime;
        booking.withDriver = withDriver;
        booking.price = price;
        booking.travelers = drivers;
        return booking;
    }

    public static Booking forProperty(String userId, String contactEmail, ProviderType providerType,
                                      String providerOfferId, String title, String propertyType, String city,
                                      String country, int bedrooms, int maxGuests, boolean entirePlace,
                                      LocalDate checkIn, LocalDate checkOut, Money price,
                                      List<BookedTraveler> guests) {
        Booking booking = new Booking();
        booking.userId = userId;
        booking.contactEmail = contactEmail;
        booking.offerType = OfferType.FURNISHED_RENTAL;
        booking.providerType = providerType;
        booking.providerOfferId = providerOfferId;
        booking.propertyTitle = title;
        booking.propertyType = propertyType;
        booking.cityCode = city;
        booking.country = country;
        booking.bedrooms = bedrooms;
        booking.maxGuests = maxGuests;
        booking.entirePlace = entirePlace;
        booking.checkIn = checkIn;
        booking.checkOut = checkOut;
        booking.price = price;
        booking.travelers = guests;
        return booking;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // =========================================================================
    // Logique Métier & Transitions d'état
    // =========================================================================

    public void assignReseller(String resellerId) {
        this.resellerId = resellerId;
    }

    public void assignPartner(String partnerId) {
        this.partnerId = partnerId;
    }

    public void applyPaymentPlan(PaymentPlan paymentPlan, Money reservationFee) {
        this.paymentPlan = paymentPlan;
        this.reservationFee = reservationFee;
    }

    /** Records what's needed to retry the provider hold later without re-asking the guest. */
    public void assignRetryContext(String searchOfferId, String contactPhone) {
        this.searchOfferId = searchOfferId;
        this.contactPhone = contactPhone;
    }

    /** The provider hold succeeded: moves out of {@code PENDING_HOLD} into the ordinary payment flow. */
    public void markOnHold(String pnrCode, LocalDateTime ticketingDeadline) {
        this.providerConfirmationNumber = pnrCode;
        this.ticketingDeadline = ticketingDeadline;
        this.status = BookingStatus.PENDING_PAYMENT;
    }

    /** The provider hold succeeded: moves out of {@code PENDING_HOLD} into the ordinary payment flow. */
    public void markOnHoldMultiLeg(List<String> pnrCodes, LocalDateTime earliestTicketingDeadline) {
        this.legPnrCodes = pnrCodes;
        if (!pnrCodes.isEmpty()) {
            this.providerConfirmationNumber = pnrCodes.get(0);
        }
        this.ticketingDeadline = earliestTicketingDeadline;
        this.status = BookingStatus.PENDING_PAYMENT;
    }

    public List<String> pnrCodes() {
        return legPnrCodes.isEmpty() ? List.of(providerConfirmationNumber) : legPnrCodes;
    }

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

    /** A failed hold, never confirmed with the provider, can be resubmitted from scratch. */
    public boolean canRetryHold() {
        return status == BookingStatus.FAILED && providerConfirmationNumber == null;
    }

    public void markRetrying() {
        this.status = BookingStatus.PENDING_HOLD;
        this.failureReason = null;
    }
}