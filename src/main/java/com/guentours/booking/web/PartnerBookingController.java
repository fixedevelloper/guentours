package com.guentours.booking.web;

import com.guentours.booking.domain.BookingRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Lives in the {@code booking} module (like {@link AdminBookingController}) rather than
 * {@code partners}, even though every caller is a partner account: a {@code partners -> booking}
 * dependency here used to close a {@code booking -> provider -> partners -> booking} cycle that
 * Spring Modulith's {@code ModularityTests} flagged. Booking-listing endpoints belong to the
 * module that owns {@link BookingRepository}/{@link BookingResponse}, regardless of which actor
 * (admin, partner, customer) consumes them - the route path is unaffected by the package move.
 */
@RestController
@RequestMapping("/api/partners/{partnerId}/bookings")
public class PartnerBookingController {

    private final BookingRepository bookingRepository;

    public PartnerBookingController(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    /**
     * Maps to {@link BookingResponse} rather than returning the {@link com.guentours.booking.domain.Booking}
     * entity directly - the entity's {@code travelers} carry passport number, date of birth and
     * nationality (see {@link com.guentours.booking.domain.BookedTraveler}), plus internal fields
     * (userId, contactPhone, resellerId, PNR codes) a partner has no business need to see. {@link BookingResponse}
     * already strips all of that down to {@code fullName/type/seatNumber} per traveler - the same
     * DTO the customer's own dashboard and the admin back-office use.
     */
    @GetMapping
    @PreAuthorize("#partnerId == authentication.principal.partnerId")
    public Page<BookingResponse> list(@PathVariable String partnerId, Pageable pageable) {
        return bookingRepository.findByPartnerId(partnerId, pageable).map(BookingResponse::from);
    }
}
