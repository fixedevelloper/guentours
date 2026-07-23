package com.guentours.partners.flight.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "flight_availabilities", uniqueConstraints =
    @UniqueConstraint(columnNames = {"flight_fare_id", "flight_date"}))
public class FlightAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_fare_id", nullable = false)
    private FlightFare fare;

    @Column(nullable = false)
    private LocalDate flightDate;

    @Column(nullable = false)
    private Integer seatsAvailable;

    private BigDecimal priceOverride;

    @Enumerated(EnumType.STRING)
    private DepartureStatus status = DepartureStatus.SCHEDULED;

    protected FlightAvailability() {}

    public FlightAvailability(FlightFare fare, LocalDate flightDate, Integer seatsAvailable) {
        this.fare = fare;
        this.flightDate = flightDate;
        this.seatsAvailable = seatsAvailable;
    }

    public void decrementSeats(int qty) {
        if (seatsAvailable < qty) throw new IllegalStateException("Sièges insuffisants");
        seatsAvailable -= qty;
    }

    public String getId() { return id; }
    public FlightFare getFare() { return fare; }
    public LocalDate getFlightDate() { return flightDate; }
    public Integer getSeatsAvailable() { return seatsAvailable; }
    public BigDecimal getPriceOverride() { return priceOverride; }
    public DepartureStatus getStatus() { return status; }
}
