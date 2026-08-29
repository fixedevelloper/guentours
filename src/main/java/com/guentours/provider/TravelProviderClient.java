package com.guentours.provider;

import com.guentours.provider.dto.FlightPriceVerification;
import com.guentours.provider.dto.HotelPriceVerification;
import com.guentours.provider.dto.PropertyPriceVerification;
import com.guentours.provider.dto.VehiclePriceVerification;

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

    /**
     * Fetches an additional page of hotel results for a search already performed via
     * {@link #searchHotels}. {@code searchIdentifier} is whatever pagination token this same
     * adapter captured on its page-1 offers (see {@link HotelOffer#context}, key
     * {@code "searchIdentifier"}) - the caller (see {@code HotelSearchService}) only calls this
     * for providers that actually captured one, so an adapter never has to guard against a null
     * identifier it doesn't understand. Must never throw for a plain "no more results"/timeout -
     * return an empty list instead, exactly like {@link #searchHotels}.
     *
     * <p>Default returns an empty list: only adapters whose upstream API actually supports
     * incremental pagination (e.g. Travelport's {@code GET
     * /hotel/search/properties/{identifier}?pageNumber=N}) need to override this.
     */
    default List<HotelOffer> loadMoreHotels(HotelSearchCriteria criteria, String searchIdentifier, int pageNumber) {
        return List.of();
    }
// Ajouts dans TravelProviderClient, à la suite de searchHotels

    /**
     * Must never throw for a plain "no results"/timeout - return an empty list instead.
     * Default empty implementation: providers without car rental inventory don't need to override this.
     */
    default List<VehicleOffer> searchVehicles(VehicleSearchCriteria criteria) {
        return List.of();
    }

    /**
     * Must never throw for a plain "no results"/timeout - return an empty list instead.
     * Default empty implementation: providers without furnished-rental inventory don't need to override this.
     */
    default List<PropertyOffer> searchProperties(PropertySearchCriteria criteria) {
        return List.of();
    }
    /**
     * Seat map for a previously-searched flight offer, used by the seat-selection step. Returns
     * {@code null} by default (provider exposes no seat data); adapters that integrate a real
     * seat-map API override this. Callers fall back to a generic simulated map when this is
     * {@code null} or empty. Must never throw for a plain "no seat data"/timeout - return
     * {@code null} instead.
     */
    default ProviderSeatMap seatMap(FlightOffer offer) {
        return null;
    }

    /**
     * Priced extras (baggage, meals, paid seats) available for {@code offer}, if this provider
     * exposes any before booking. {@code passengers} carries only basic demographics (name, type)
     * - some providers require them to price ancillaries at all (e.g. Travel Terminus's Pre
     * Ancillary sequence lock). Default returns an empty list: providers without a pre-booking
     * ancillary API don't need to override this. Must never throw for a plain "unavailable" case
     * - return an empty list instead.
     */
    default List<AncillaryOption> ancillaryOptions(FlightOffer offer, List<PassengerInfo> passengers) {
        return List.of();
    }

    // ==========================================
    // 2. VALIDATION & TARIFICATION REEL (Price & Rule Check)
    // ==========================================

    /**
     * Re-validates the flight offer price, baggage rules, and seat availability
     * directly with the GDS before collecting passenger details. Receives the full
     * cached offer (not just its id) because most vendors' revalidation APIs (e.g.
     * Sabre's Revalidate Itinerary) re-price by itinerary details - carrier, flight
     * number, segment date/times - rather than by an opaque offer reference.
     *
     * @throws OfferExpiredException if the seats are no longer available or price changed.
     */
    FlightPriceVerification verifyFlightPrice(FlightOffer offer);

    /**
     * Verifies current hotel room availability and final tax inclusions before checkout. Receives
     * the full cached offer (not just its id) because availability re-checks re-query by property
     * details (e.g. Travelport's chain/property codes) rather than an opaque offer reference.
     * {@code roomQuantity} is how many rooms the guest is paying for; {@link HotelOffer#price()} is
     * per-room, so implementations that re-query a total price must normalize it back to a per-room
     * price before comparing.
     */
    HotelPriceVerification verifyHotelPrice(HotelOffer offer, int roomQuantity);
    HotelDetail getDetailHotel(HotelOffer offer);
    List<RoomOffer> getRoomOffers(HotelOffer offer);
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

    /**
     * Places a hold on the vehicle for the requested rental period. Default throws
     * UnsupportedOperationException - only adapters that actually support car rental
     * inventory (see searchVehicles) need to override this.
     */
    default ProviderBookingConfirmation createVehicleHold(VehicleBookingRequest request) {
        throw new UnsupportedOperationException(getType() + " does not support vehicle bookings");
    }

    /**
     * Places a hold on the property for the requested stay. Default throws
     * UnsupportedOperationException - only adapters that actually support furnished-rental
     * inventory (see searchProperties) need to override this.
     */
    default ProviderBookingConfirmation createPropertyHold(PropertyBookingRequest request) {
        throw new UnsupportedOperationException(getType() + " does not support property bookings");
    }

    default void cancelVehicleBooking(String bookingRef) {
        throw new UnsupportedOperationException(getType() + " does not support vehicle bookings");
    }

    default void cancelPropertyBooking(String bookingRef) {
        throw new UnsupportedOperationException(getType() + " does not support property bookings");
    }

    /**
     * Re-validates vehicle price/availability before the hold. Default throws
     * UnsupportedOperationException - only adapters that support car rental inventory
     * (see searchVehicles) need to override this.
     */
    default VehiclePriceVerification verifyVehiclePrice(VehicleOffer offer) {
        throw new UnsupportedOperationException(getType() + " does not support vehicle bookings");
    }

    /**
     * Re-validates property price/availability before the hold. Default throws
     * UnsupportedOperationException - only adapters that support furnished-rental inventory
     * (see searchProperties) need to override this.
     */
    default PropertyPriceVerification verifyPropertyPrice(PropertyOffer offer) {
        throw new UnsupportedOperationException(getType() + " does not support property bookings");
    }
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

    /**
     * Re-checks whether e-tickets have since been issued for an already-CONFIRMED flight booking
     * whose {@link #issueFlightTicket} call returned {@code issued=true} but no ticket numbers yet
     * - Travel Terminus can ticket asynchronously on its own ~10-minute reconciliation cadence, well
     * past issueFlightTicket's short poll window. Called periodically by
     * {@code ETicketReconciliationJob}, never during checkout itself. Returns an empty list if
     * still not available.
     *
     * <p>Default: providers that always ticket synchronously in issueFlightTicket never leave
     * anything to reconcile, so there's nothing to check here.
     */
    default List<String> checkForIssuedTickets(String providerConfirmationNumber) {
        return List.of();
    }

    /**
     * Live supplementary flight order details (baggage/meals/seats per traveler, cancellation
     * policy, current provider-side booking status) beyond what's already stored on the {@code
     * Booking} itself - powers the flight booking detail page. Not persisted: fetched on demand
     * since it reflects the provider's current state, not a point-in-time snapshot.
     *
     * <p>Default: providers without a richer order-details API return {@code null}; callers must
     * treat that as "extra detail unavailable" and fall back to what's already on the Booking.
     */
    default FlightOrderDetail getFlightOrderDetail(String providerConfirmationNumber) {
        return null;
    }

    // ==========================================
    // 5. APRÈS-VENTE (Void / Cancel)
    // ==========================================

    /**
     * Cancels a booking or voids a ticket if within the allowed cancellation window.
     */
    void cancelFlightBooking(String pnrCode);

    /**
     * Cancels an existing hotel reservation based on the provider's cancellation policy.
     * {@code providerOfferId} and {@code supplierLocator} are Travelport-only extras its
     * cancellation call needs alongside the confirmation - ignored by every other provider.
     */
    void cancelHotelBooking(String hotelBookingRef, String providerOfferId, String supplierLocator);
}
