package com.guentours.ticketing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ETicketRepository extends JpaRepository<ETicket, String> {

    List<ETicket> findByBookingId(String bookingId);
}
