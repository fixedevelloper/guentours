package com.guentours.partners.web;

import com.guentours.booking.Booking;
import com.guentours.booking.BookingRepository;
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

    @GetMapping
    @PreAuthorize("#partnerId == authentication.principal.partnerId")
    public Page<Booking> list(@PathVariable String partnerId, Pageable pageable) {
        return bookingRepository.findByPartnerId(partnerId, pageable);
    }
}