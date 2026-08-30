package com.guentours.payment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    List<Payment> findByStatus(PaymentStatus status);

    Optional<Payment> findByBookingIdAndStatus(String bookingId, PaymentStatus status);
}
