package com.guentours.partners.carrental.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String partnerId;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Transmission transmission;

    @Column(nullable = false)
    private Integer seats;

    @Column(nullable = false)
    private Boolean airConditioning;

    @Column(nullable = false)
    private BigDecimal pricePerDay;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private Integer unitsCount;

    @ElementCollection
    @CollectionTable(name = "vehicle_pickup_locations", joinColumns = @JoinColumn(name = "vehicle_id"))
    @Column(name = "city")
    private List<String> pickupLocations;

    @Enumerated(EnumType.STRING)
    private ListingStatus status = ListingStatus.ACTIVE;

    protected Vehicle() {}

    public Vehicle(String partnerId, String brand, String model, Integer year,
                   VehicleCategory category, Transmission transmission, Integer seats,
                   Boolean airConditioning, BigDecimal pricePerDay, String currency,
                   Integer unitsCount, List<String> pickupLocations) {
        this.partnerId = partnerId;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.category = category;
        this.transmission = transmission;
        this.seats = seats;
        this.airConditioning = airConditioning;
        this.pricePerDay = pricePerDay;
        this.currency = currency;
        this.unitsCount = unitsCount;
        this.pickupLocations = pickupLocations;
    }

    public void suspend() { this.status = ListingStatus.SUSPENDED; }
    public void activate() { this.status = ListingStatus.ACTIVE; }

    public String getId() { return id; }
    public String getPartnerId() { return partnerId; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public Integer getYear() { return year; }
    public VehicleCategory getCategory() { return category; }
    public Transmission getTransmission() { return transmission; }
    public Integer getSeats() { return seats; }
    public Boolean getAirConditioning() { return airConditioning; }
    public BigDecimal getPricePerDay() { return pricePerDay; }
    public String getCurrency() { return currency; }
    public Integer getUnitsCount() { return unitsCount; }
    public List<String> getPickupLocations() { return pickupLocations; }
    public ListingStatus getStatus() { return status; }
}
