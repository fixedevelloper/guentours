package com.guentours.partners.flight.domain;

import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.Set;

@Entity
@Table(name = "airline_flights")
public class AirlineFlight {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String partnerId;

    @Column(nullable = false)
    private String flightNumber;

    @Column(nullable = false)
    private String aircraftType;

    @Column(nullable = false, length = 3)
    private String originAirportCode;

    @Column(nullable = false, length = 3)
    private String destinationAirportCode;

    @Column(nullable = false)
    private LocalTime departureTime;

    @Column(nullable = false)
    private LocalTime arrivalTime;

    @Column(nullable = false)
    private Integer durationMinutes;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "airline_flight_operating_days", joinColumns = @JoinColumn(name = "flight_id"))
    @Column(name = "day_of_week")
    private Set<Integer> operatingDays;

    @Enumerated(EnumType.STRING)
    private FlightStatus status = FlightStatus.ACTIVE;

    protected AirlineFlight() {}

    public AirlineFlight(String partnerId, String flightNumber, String aircraftType,
                          String originAirportCode, String destinationAirportCode,
                          LocalTime departureTime, LocalTime arrivalTime,
                          Integer durationMinutes, Set<Integer> operatingDays) {
        this.partnerId = partnerId;
        this.flightNumber = flightNumber;
        this.aircraftType = aircraftType;
        this.originAirportCode = originAirportCode;
        this.destinationAirportCode = destinationAirportCode;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.durationMinutes = durationMinutes;
        this.operatingDays = operatingDays;
    }

    public void suspend() { this.status = FlightStatus.SUSPENDED; }
    public void activate() { this.status = FlightStatus.ACTIVE; }

    public String getId() { return id; }
    public String getPartnerId() { return partnerId; }
    public String getFlightNumber() { return flightNumber; }
    public String getAircraftType() { return aircraftType; }
    public String getOriginAirportCode() { return originAirportCode; }
    public String getDestinationAirportCode() { return destinationAirportCode; }
    public LocalTime getDepartureTime() { return departureTime; }
    public LocalTime getArrivalTime() { return arrivalTime; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public Set<Integer> getOperatingDays() { return operatingDays; }
    public FlightStatus getStatus() { return status; }

}
