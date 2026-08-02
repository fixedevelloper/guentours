package com.guentours.booking.domain;

/** Projection for {@link BookingRepository#countFlightBookingsByDestination}. */
public interface DestinationBookingCount {
    String getDestinationCode();
    long getBookingCount();
}
