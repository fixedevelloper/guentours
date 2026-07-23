package com.guentours.partners.carrental.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "vehicle_availabilities", uniqueConstraints =
    @UniqueConstraint(columnNames = {"vehicle_id", "rent_date"}))
public class VehicleAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(nullable = false)
    private LocalDate rentDate;

    @Column(nullable = false)
    private Integer unitsAvailable;

    private BigDecimal priceOverride;

    protected VehicleAvailability() {}

    public VehicleAvailability(Vehicle vehicle, LocalDate rentDate, Integer unitsAvailable) {
        this.vehicle = vehicle;
        this.rentDate = rentDate;
        this.unitsAvailable = unitsAvailable;
    }

    public void decrementUnits(int qty) {
        if (unitsAvailable < qty) throw new IllegalStateException("Véhicules insuffisants");
        unitsAvailable -= qty;
    }

    public String getId() { return id; }
    public Vehicle getVehicle() { return vehicle; }
    public LocalDate getRentDate() { return rentDate; }
    public Integer getUnitsAvailable() { return unitsAvailable; }
    public BigDecimal getPriceOverride() { return priceOverride; }
}
