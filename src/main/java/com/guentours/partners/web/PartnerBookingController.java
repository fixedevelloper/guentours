package com.guentours.partners.web;

import com.guentours.booking.domain.BookingRepository;
import com.guentours.booking.web.BookingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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