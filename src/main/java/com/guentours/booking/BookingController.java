package com.guentours.booking;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
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

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBooking(@PathVariable String id) {
        return ResponseEntity.ok(BookingResponse.from(bookingService.getById(id)));
    }

    /** Voids the provider's PNR/reservation and marks the booking cancelled. */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancel(@PathVariable String id) {
        return ResponseEntity.ok(BookingResponse.from(bookingService.cancel(id)));
    }

    /** Server-Sent Events stream of status transitions: PENDING_PAYMENT -> PAID -> CONFIRMING -> CONFIRMED/FAILED. */
    @GetMapping("/{id}/track")
    public SseEmitter track(@PathVariable String id) {
        return bookingService.track(id);
    }
}
