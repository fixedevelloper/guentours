package com.guentours.booking.web;

import com.guentours.booking.BookingService;
import com.guentours.booking.domain.Booking;
import com.guentours.provider.FlightOrderDetail;
import com.guentours.security.SecurityUtils;
import com.guentours.shared.exception.NotFoundException;
import com.guentours.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final UserService userService;

    public BookingController(BookingService bookingService, UserService userService) {
        this.bookingService = bookingService;
        this.userService = userService;
    }

    /** Every booking made by the signed-in account, most recent first - backs the customer dashboard. */
    @GetMapping("/me")
    public ResponseEntity<List<BookingResponse>> myBookings() {
        String email = SecurityUtils.currentUserEmail();
        if (email == null) {
            throw new NotFoundException("Not authenticated");
        }
        String userId = userService.getByEmail(email).getId();
        List<BookingResponse> bookings = bookingService.getForUser(userId).stream()
                .map(BookingResponse::from)
                .toList();
        return ResponseEntity.ok(bookings);
    }

    /** Registers the booking (still unpaid) from a harmonized search offer, auto-provisioning the account if needed. */
    @PostMapping("/checkout")
    public ResponseEntity<BookingResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        Booking booking = bookingService.checkout(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingResponse.from(booking));
    }

    /** Registers every leg of a MULTI_CITY itinerary (same provider) as a single booking. */
    @PostMapping("/checkout/multi-city")
    public ResponseEntity<BookingResponse> checkoutMultiCity(@Valid @RequestBody MultiCityCheckoutRequest request) {
        Booking booking = bookingService.checkoutMultiCity(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingResponse.from(booking));
    }

    /**
     * Quotes priced extras (baggage/meal/seat/insurance) for the "additional options" checkout
     * step, ahead of the final {@code /checkout} submission. Returns an empty list for offer types
     * other than FLIGHT, or when the provider exposes no ancillaries for this offer.
     */
    @PostMapping("/ancillary-options")
    public ResponseEntity<List<AncillaryOptionResponse>> ancillaryOptions(@Valid @RequestBody AncillaryOptionsRequest request) {
        return ResponseEntity.ok(bookingService.ancillaryOptions(request));
    }

    /**
     * {@code email} is required for anonymous/guest access (must match the booking's contact
     * email) - not needed when authenticated as the owning account or as an admin.
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable String id,
                                                      @RequestParam(required = false) String email) {
        Booking booking = bookingService.getById(id);
        bookingService.verifyGuestAccess(booking, email);
        return ResponseEntity.ok(BookingResponse.from(booking));
    }

    /**
     * Resubmits the provider hold for a booking that failed before ever getting a PNR/confirmation
     * number (see {@link Booking#canRetryHold}) - lets the guest retry without redoing checkout.
     */
    @PostMapping("/{id}/retry")
    public ResponseEntity<BookingResponse> retry(@PathVariable String id,
                                                 @RequestParam(required = false) String email) {
        bookingService.verifyGuestAccess(bookingService.getById(id), email);
        return ResponseEntity.accepted().body(BookingResponse.from(bookingService.retryHold(id)));
    }

    /** Voids the provider's PNR/reservation and marks the booking cancelled. */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancel(@PathVariable String id,
                                                  @RequestParam(required = false) String email) {
        bookingService.verifyGuestAccess(bookingService.getById(id), email);
        return ResponseEntity.ok(BookingResponse.from(bookingService.cancel(id)));
    }

    /**
     * Live baggage/meals/seats/cancellation-policy detail for a confirmed flight booking, fetched
     * fresh from the provider (see {@link BookingService#getFlightOrderDetail}) - powers the flight
     * detail page. Body is {@code null} when unavailable (not a flight, not yet provider-confirmed,
     * or the provider doesn't support this) - the frontend falls back to what's already in the
     * booking response itself.
     */
    @GetMapping("/{id}/flight-order-detail")
    public ResponseEntity<FlightOrderDetail> flightOrderDetail(@PathVariable String id,
                                                                @RequestParam(required = false) String email) {
        Booking booking = bookingService.getById(id);
        bookingService.verifyGuestAccess(booking, email);
        return ResponseEntity.ok(bookingService.getFlightOrderDetail(booking));
    }

    /** Server-Sent Events stream of status transitions: PENDING_PAYMENT -> PAID -> CONFIRMING -> CONFIRMED/FAILED. */
    @GetMapping("/{id}/track")
    public SseEmitter track(@PathVariable String id, @RequestParam(required = false) String email) {
        bookingService.verifyGuestAccess(bookingService.getById(id), email);
        return bookingService.track(id);
    }
}
