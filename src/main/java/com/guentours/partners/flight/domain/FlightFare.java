package com.guentours.partners.flight.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "flight_fares")
public class FlightFare {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flight_id", nullable = false)
    private AirlineFlight flight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CabinClass cabinClass;

    @Column(nullable = false)
    private BigDecimal basePrice;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private Integer baggageAllowanceKg;

    @Column(nullable = false)
    private Integer totalSeats;

    protected FlightFare() {}

    public FlightFare(AirlineFlight flight, CabinClass cabinClass, BigDecimal basePrice,
                       String currency, Integer baggageAllowanceKg, Integer totalSeats) {
        this.flight = flight;
        this.cabinClass = cabinClass;
        this.basePrice = basePrice;
        this.currency = currency;
        this.baggageAllowanceKg = baggageAllowanceKg;
        this.totalSeats = totalSeats;
    }

    public String getId() { return id; }
    public AirlineFlight getFlight() { return flight; }
    public CabinClass getCabinClass() { return cabinClass; }
    public BigDecimal getBasePrice() { return basePrice; }
    public String getCurrency() { return currency; }
    public Integer getBaggageAllowanceKg() { return baggageAllowanceKg; }
    public Integer getTotalSeats() { return totalSeats; }
}
