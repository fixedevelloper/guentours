package com.guentours.booking.web;

import com.guentours.booking.BookingTravelerResponse;
import com.guentours.booking.domain.Booking;
import com.guentours.booking.domain.BookingStatus;
import com.guentours.booking.domain.OfferType;
import com.guentours.booking.domain.PaymentPlan;
import com.guentours.provider.ProviderType;
import com.guentours.shared.Money;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record BookingResponse(
        String id,
        BookingStatus status,
        OfferType offerType,
        ProviderType providerType,
        String contactEmail,
        Money price,
        PaymentPlan paymentPlan,
        Money reservationFee,
        Money amountDue,
        LocalDateTime ticketingDeadline,
        String providerConfirmationNumber,
        List<String> eTicketNumbers,
        List<BookingFlightLegResponse> itineraryLegs,
        String failureReason,
        List<BookingTravelerResponse> travelers,
        String airline,
        String flightNumber,
        String origin,
        String destination,
        LocalDateTime departureTime,
        LocalDateTime arrivalTime,
        String hotelName,
        String cityCode,
        LocalDate checkIn,
        LocalDate checkOut,
        String fareClass,
        String vehicleBrand,
        String vehicleModel,
        String vehicleCategory,
        String vehicleTransmission,
        Integer vehicleSeats,
        String pickupCity,
        String dropoffCity,
        LocalDate rentalStart,
        LocalTime pickupTime,
        LocalDate rentalEnd,
        LocalTime dropoffTime,
        Boolean withDriver,
        String propertyTitle,
        String propertyType,
        String country,
        Integer bedrooms,
        Integer maxGuests,
        Boolean entirePlace,
        Instant createdAt
) {
    public static BookingResponse from(Booking booking) {
        List<BookingFlightLegResponse> legs = booking.getItineraryLegs().stream()
                .map(BookingFlightLegResponse::from)
                .toList();
        List<BookingTravelerResponse> travelers = booking.getTravelers().stream()
                .map(BookingTravelerResponse::from)
                .toList();
        return new BookingResponse(booking.getId(), booking.getStatus(), booking.getOfferType(),
                booking.getProviderType(), booking.getContactEmail(), booking.getPrice(), booking.getPaymentPlan(), booking.getReservationFee(),
                booking.amountDue(), booking.getTicketingDeadline(), booking.getProviderConfirmationNumber(),
                booking.getETicketNumbers(), legs, booking.getFailureReason(), travelers,
                booking.getAirline(), booking.getFlightNumber(), booking.getOrigin(), booking.getDestination(),
                booking.getDepartureTime(), booking.getArrivalTime(), booking.getHotelName(), booking.getCityCode(),
                booking.getCheckIn(), booking.getCheckOut(), booking.getFareClass(),
                booking.getVehicleBrand(), booking.getVehicleModel(), booking.getVehicleCategory(),
                booking.getVehicleTransmission(), booking.getVehicleSeats(), booking.getPickupCity(),
                booking.getDropoffCity(), booking.getRentalStart(), booking.getPickupTime(),
                booking.getRentalEnd(), booking.getDropoffTime(), booking.getWithDriver(),
                booking.getPropertyTitle(), booking.getPropertyType(), booking.getCountry(),
                booking.getBedrooms(), booking.getMaxGuests(), booking.getEntirePlace(),
                booking.getCreatedAt());
    }
}
