package com.guentours.ticketing;

import com.guentours.booking.domain.Booking;
import com.guentours.booking.event.BookingConfirmedEvent;
import com.guentours.booking.BookingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ETicketService {

    private static final Logger log = LoggerFactory.getLogger(ETicketService.class);

    private final ETicketRepository eTicketRepository;
    private final BookingService bookingService;

    public ETicketService(ETicketRepository eTicketRepository, BookingService bookingService) {
        this.eTicketRepository = eTicketRepository;
        this.bookingService = bookingService;
    }

    @ApplicationModuleListener
    void on(BookingConfirmedEvent event) {
        Booking booking = bookingService.getById(event.bookingId());
        for (String ticketNumber : booking.getETicketNumbers()) {
            String document = renderDocument(booking, ticketNumber);
            eTicketRepository.save(new ETicket(booking.getId(), ticketNumber, booking.getProviderConfirmationNumber(), document));
        }
        log.info("Generated {} e-ticket(s) for booking {}", booking.getETicketNumbers().size(), booking.getId());
    }

    public List<ETicket> getForBooking(String bookingId, String email) {
        Booking booking = bookingService.getById(bookingId);
        bookingService.verifyGuestAccess(booking, email);
        return eTicketRepository.findByBookingId(bookingId);
    }

    private String renderDocument(Booking booking, String ticketNumber) {
        return """
                =====================================
                        ELECTRONIC TICKET
                =====================================
                Ticket number:      %s
                Confirmation code:  %s
                Passenger contact:  %s
                Booking reference:  %s
                Type:                %s
                =====================================
                """.formatted(ticketNumber, booking.getProviderConfirmationNumber(), booking.getContactEmail(),
                booking.getId(), booking.getOfferType());
    }
}
