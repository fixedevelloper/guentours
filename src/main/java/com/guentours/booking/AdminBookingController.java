package com.guentours.booking;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin-only read access to every booking (see {@code /api/admin/**} in SecurityConfig). */
@RestController
@RequestMapping("/api/admin/bookings")
public class AdminBookingController {

    private final BookingService bookingService;

    public AdminBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public ResponseEntity<List<BookingResponse>> allBookings() {
        List<BookingResponse> bookings = bookingService.getAll().stream()
                .map(BookingResponse::from)
                .toList();
        return ResponseEntity.ok(bookings);
    }
}
