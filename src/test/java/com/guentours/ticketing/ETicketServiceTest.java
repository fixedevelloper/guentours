package com.guentours.ticketing;

import com.guentours.booking.BookingService;
import com.guentours.booking.domain.Booking;
import com.guentours.booking.event.BookingConfirmedEvent;
import com.guentours.provider.ProviderType;
import com.guentours.shared.Money;
import com.guentours.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ETicketServiceTest {

    @Mock
    private ETicketRepository eTicketRepository;
    @Mock
    private BookingService bookingService;

    private ETicketService eTicketService;

    @BeforeEach
    void setUp() {
        eTicketService = new ETicketService(eTicketRepository, bookingService);
    }

    private Booking confirmedFlightBooking(List<String> eTicketNumbers) {
        Booking booking = Booking.forFlight("user-1", "jean@example.com", ProviderType.DIRECT, "offer-1",
                "AF", "AF123", "CDG", "JFK", LocalDateTime.now().plusDays(10),
                LocalDateTime.now().plusDays(10).plusHours(9), "ECONOMY", Money.of(150_000, "XAF"), List.of());
        booking.markConfirmed("PNR123", eTicketNumbers);
        return booking;
    }

    @Test
    @DisplayName("on(BookingConfirmedEvent) saves one ETicket per e-ticket number, each with a rendered document")
    void generatesOneETicketPerNumber() {
        Booking booking = confirmedFlightBooking(List.of("ET-001", "ET-002"));
        when(bookingService.getById("booking-1")).thenReturn(booking);

        eTicketService.on(new BookingConfirmedEvent("booking-1"));

        ArgumentCaptor<ETicket> savedCaptor = ArgumentCaptor.forClass(ETicket.class);
        verify(eTicketRepository, times(2)).save(savedCaptor.capture());

        List<ETicket> saved = savedCaptor.getAllValues();
        assertThat(saved).extracting(ETicket::getTicketNumber).containsExactly("ET-001", "ET-002");
        assertThat(saved).allSatisfy(ticket -> {
            assertThat(ticket.getBookingId()).isEqualTo(booking.getId());
            assertThat(ticket.getProviderConfirmationNumber()).isEqualTo("PNR123");
            assertThat(ticket.getDocument()).contains("PNR123", "jean@example.com", "FLIGHT");
        });
    }

    @Test
    @DisplayName("on(BookingConfirmedEvent) saves nothing for a booking with no e-ticket numbers")
    void savesNothingWhenNoETicketNumbers() {
        Booking booking = confirmedFlightBooking(List.of());
        when(bookingService.getById("booking-2")).thenReturn(booking);

        eTicketService.on(new BookingConfirmedEvent("booking-2"));

        verifyNoInteractions(eTicketRepository);
    }

    @Test
    @DisplayName("getForBooking returns the repository's tickets once guest access is verified")
    void getForBookingReturnsTicketsAfterAccessCheck() {
        Booking booking = confirmedFlightBooking(List.of("ET-001"));
        when(bookingService.getById("booking-1")).thenReturn(booking);
        List<ETicket> tickets = List.of(new ETicket("booking-1", "ET-001", "PNR123", "doc"));
        when(eTicketRepository.findByBookingId("booking-1")).thenReturn(tickets);

        List<ETicket> result = eTicketService.getForBooking("booking-1", "jean@example.com");

        assertThat(result).isEqualTo(tickets);
        verify(bookingService).verifyGuestAccess(booking, "jean@example.com");
    }

    @Test
    @DisplayName("getForBooking propagates the access-denied failure instead of leaking another guest's tickets")
    void getForBookingPropagatesAccessDenied() {
        Booking booking = confirmedFlightBooking(List.of("ET-001"));
        when(bookingService.getById("booking-1")).thenReturn(booking);
        doThrow(new NotFoundException("Booking not found: booking-1"))
                .when(bookingService).verifyGuestAccess(booking, "wrong@example.com");

        assertThatThrownBy(() -> eTicketService.getForBooking("booking-1", "wrong@example.com"))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(eTicketRepository);
    }
}
