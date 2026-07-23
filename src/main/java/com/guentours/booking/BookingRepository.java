package com.guentours.booking;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, String> {

    List<Booking> findByUserId(String userId);

    /** Holds whose provider deadline has lapsed without being fully paid - candidates for auto-cancellation. */
    List<Booking> findByStatusInAndTicketingDeadlineBefore(List<BookingStatus> statuses, LocalDateTime cutoff);
    Page<Booking> findByPartnerId(String partnerId, Pageable pageable);
}
