package com.guentours.notification.web;

import com.guentours.notification.BookingConfirmationMailer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only "Envoyer Email" action on the booking detail page - resends the same confirmation
 * email {@code notification.NotificationEventListener} already sends automatically once a
 * booking reaches CONFIRMED. Lives in the notification module rather than
 * {@code booking.web.AdminBookingController} because Spring Modulith only allows notification to
 * depend on booking, not the reverse (same reasoning as {@code payment.web.AdminPaymentController}).
 */
@RestController
@RequestMapping("/api/admin/bookings")
public class AdminBookingNotificationController {

    private final BookingConfirmationMailer bookingConfirmationMailer;

    public AdminBookingNotificationController(BookingConfirmationMailer bookingConfirmationMailer) {
        this.bookingConfirmationMailer = bookingConfirmationMailer;
    }

    @PostMapping("/{id}/resend-confirmation")
    public ResponseEntity<Void> resendConfirmation(@PathVariable String id) {
        bookingConfirmationMailer.resend(id);
        return ResponseEntity.noContent().build();
    }
}
