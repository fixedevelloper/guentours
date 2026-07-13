package com.guentours.provider;

import com.guentours.provider.dto.FlightPriceVerification;
import com.guentours.provider.dto.HotelPriceVerification;

import java.util.List;

/**
 * Single-Provider-Interface (SPI) implemented once per external GDS/aggregator
 * (Travelopro, Sabre, Travelport, ...).
 *
 * Defines the complete end-to-end booking lifecycle for flights and hotels.
 */
public interface TravelProviderClient {

    ProviderType getType();
    boolean isEnabled();

    // ==========================================
    // 1. RECHERCHE (Search & Shop)
    // ==========================================

    /** Must never throw for a plain "no results"/timeout - return an empty list instead. */
    List<FlightOffer> searchFlights(FlightSearchCriteria criteria);

    /** Must never throw for a plain "no results"/timeout - return an empty list instead. */
    List<HotelOffer> searchHotels(HotelSearchCriteria criteria);

    // ==========================================
    // 2. VALIDATION & TARIFICATION REEL (Price & Rule Check)
    // ==========================================

    /**
     * Re-validates the flight offer price, baggage rules, and seat availability
     * directly with the GDS before collecting passenger details.
     *
     * @throws OfferExpiredException if the seats are no longer available or price changed.
     */
    FlightPriceVerification verifyFlightPrice(String flightOfferId);

    /**
     * Verifies current hotel room availability and final tax inclusions before checkout.
     */
    HotelPriceVerification verifyHotelPrice(String hotelOfferId);

    // ==========================================
    // 3. CRÉATION DE LA RÉSERVATION (Book / Hold PNR)
    // ==========================================

    /**
     * Creates a temporary booking (PNR) in the provider's system.
     * Places a hold on the seats/rooms. Passenger data is validated here.
     *
     * @return A confirmation with a reference (PNR code) and a time limit (TICKETING_DEADLINE)
     */
    ProviderBookingConfirmation createFlightHold(FlightBookingRequest request);

    /**
     * Reserves a hotel room (either on hold or immediate booking depending on provider policy).
     */
    ProviderBookingConfirmation createHotelHold(HotelBookingRequest request);

    // ==========================================
    // 4. ÉMISSION / CONFIRMATION FINALE (Ticket / Issue)
    // ==========================================

    /**
     * Triggers the actual ticketing process (issuance of e-tickets) after successful payment capture.
     * This converts a 'Hold' PNR into a final issued ticket.
     */
    FinalTicketConfirmation issueFlightTicket(String pnrCode, PaymentDetails payment);

    /**
     * Finalizes the hotel booking, capturing the room permanently.
     */
    FinalHotelConfirmation confirmHotelBooking(String hotelBookingRef, PaymentDetails payment);

    // ==========================================
    // 5. APRÈS-VENTE (Void / Cancel)
    // ==========================================

    /**
     * Cancels a booking or voids a ticket if within the allowed cancellation window.
     */
    void cancelFlightBooking(String pnrCode);

    /**
     * Cancels an existing hotel reservation based on the provider's cancellation policy.
     */
    void cancelHotelBooking(String hotelBookingRef);
}
