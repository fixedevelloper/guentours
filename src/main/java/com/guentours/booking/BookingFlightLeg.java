package com.guentours.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.time.LocalDateTime;

/** One flight segment of a MULTI_CITY booking; empty for ordinary single-leg bookings. */
@Embeddable
public class BookingFlightLeg {

    @Column(name = "leg_index")
    private int legIndex;

    private String airline;

    @Column(name = "flight_number")
    private String flightNumber;

    private String origin;

    private String destination;

    @Column(name = "departure_time")
    private LocalDateTime departureTime;

    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    protected BookingFlightLeg() {
        // JPA
    }

    public BookingFlightLeg(int legIndex, String airline, String flightNumber, String origin, String destination,
                             LocalDateTime departureTime, LocalDateTime arrivalTime) {
        this.legIndex = legIndex;
        this.airline = airline;
        this.flightNumber = flightNumber;
        this.origin = origin;
        this.destination = destination;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
    }

    public int getLegIndex() {
        return legIndex;
    }

    public String getAirline() {
        return airline;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }
}
