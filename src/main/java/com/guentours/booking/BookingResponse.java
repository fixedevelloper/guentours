package com.guentours.booking;

import com.guentours.shared.Money;

import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
        String id,
        BookingStatus status,
        OfferType offerType,
        Money price,
        PaymentPlan paymentPlan,
        Money depositAmount,
        Money amountDue,
        LocalDateTime ticketingDeadline,
        String providerConfirmationNumber,
        List<String> eTicketNumbers,
        List<BookingFlightLegResponse> itineraryLegs,
        String failureReason
) {
    public static BookingResponse from(Booking booking) {
        List<BookingFlightLegResponse> legs = booking.getItineraryLegs().stream()
                .map(BookingFlightLegResponse::from)
                .toList();
        return new BookingResponse(booking.getId(), booking.getStatus(), booking.getOfferType(), booking.getPrice(),
                booking.getPaymentPlan(), booking.getDepositAmount(), booking.amountDue(), booking.getTicketingDeadline(),
                booking.getProviderConfirmationNumber(), booking.getETicketNumbers(), legs, booking.getFailureReason());
    }
}
