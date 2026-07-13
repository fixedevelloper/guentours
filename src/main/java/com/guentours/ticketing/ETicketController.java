package com.guentours.ticketing;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class ETicketController {

    private final ETicketService eTicketService;

    public ETicketController(ETicketService eTicketService) {
        this.eTicketService = eTicketService;
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<ETicket>> getForBooking(@PathVariable String bookingId) {
        return ResponseEntity.ok(eTicketService.getForBooking(bookingId));
    }
}
