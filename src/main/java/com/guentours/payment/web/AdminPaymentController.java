package com.guentours.payment.web;

import com.guentours.payment.domain.Payment;
import com.guentours.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only refund action for a booking whose payment was captured but the provider then
 * declined final confirmation (see the {@code paymentCapturedNotice} banner on the booking
 * tracking page) - the only way to resolve that state today, since it's intentionally never
 * auto-retried (see {@code BookingService#confirmWithProvider}'s catch block). Lives in the
 * payment module rather than {@code booking.web.AdminBookingController} because Spring Modulith
 * only allows the payment module to depend on booking, not the reverse.
 */
@RestController
@RequestMapping("/api/admin/bookings")
public class AdminPaymentController {

    private final PaymentService paymentService;

    public AdminPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<Payment> refund(@PathVariable String id) {
        return ResponseEntity.ok(paymentService.refundForBooking(id));
    }
}
