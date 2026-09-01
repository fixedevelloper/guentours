package com.guentours.booking.web;

import com.guentours.booking.BookingService;
import com.guentours.booking.ReceiptDocumentService;
import com.guentours.booking.domain.Booking;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin-only read/document access for bookings (see {@code /api/admin/**} in SecurityConfig). */
@RestController
@RequestMapping("/api/admin/bookings")
public class AdminBookingController {

    private final BookingService bookingService;
    private final ReceiptDocumentService receiptDocumentService;

    public AdminBookingController(BookingService bookingService, ReceiptDocumentService receiptDocumentService) {
        this.bookingService = bookingService;
        this.receiptDocumentService = receiptDocumentService;
    }

    @GetMapping
    public ResponseEntity<List<BookingResponse>> allBookings() {
        List<BookingResponse> bookings = bookingService.getAll().stream()
                .map(BookingResponse::from)
                .toList();
        return ResponseEntity.ok(bookings);
    }

    /** Renders a fresh PDF receipt on every call (not persisted) - the "Reçu PDF" action on the
     *  admin booking detail page. */
    @PostMapping("/{id}/receipt")
    public ResponseEntity<byte[]> receipt(@PathVariable String id) {
        Booking booking = bookingService.getById(id);
        byte[] pdf = receiptDocumentService.renderPdf(booking);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"receipt-" + booking.getId() + ".pdf\"")
                .body(pdf);
    }
}
