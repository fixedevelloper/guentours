package com.guentours.ticketing;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class ETicketController {

    private final ETicketService eTicketService;

    public ETicketController(ETicketService eTicketService) {
        this.eTicketService = eTicketService;
    }

    /** {@code email} is required for guest access - must match the booking's contact email. */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<ETicket>> getForBooking(@PathVariable String bookingId,
                                                       @RequestParam(required = false) String email) {
        return ResponseEntity.ok(eTicketService.getForBooking(bookingId, email));
    }

    /** Shares a ticket's PDF by email - {@code requesterEmail} must match the booking's contact
     *  email (same guest-access rule as {@link #getForBooking}); {@code recipientEmail} is
     *  optional and defaults to the requester's own address. */
    @PostMapping("/{ticketId}/email")
    public ResponseEntity<Void> sendByEmail(@PathVariable String ticketId, @Valid @RequestBody TicketEmailRequest body) {
        eTicketService.sendByEmail(ticketId, body.requesterEmail(), body.recipientEmail());
        return ResponseEntity.noContent().build();
    }
}
