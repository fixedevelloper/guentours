package com.guentours.partners.carrental.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
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

    @Column(name = "`year`", nullable = false)
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

    // Eager : VehicleResponse.from() lit cette collection dans le contrôleur, après la fermeture
    // de la session Hibernate (open-in-view=false) - un fetch lazy y lèverait LazyInitializationException.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "vehicle_pickup_locations", joinColumns = @JoinColumn(name = "vehicle_id"))
    @Column(name = "city")
    private List<String> pickupLocations;

    @Enumerated(EnumType.STRING)
    private ListingStatus status = ListingStatus.ACTIVE;

    @Column(name = "cover_image_url", length = 1024)
    private String coverImageUrl;

    // Eager : VehicleResponse.from() lit cette collection dans le contrôleur, après la fermeture
    // de la session Hibernate (open-in-view=false) - un fetch lazy y lèverait LazyInitializationException.
    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("displayOrder ASC")
    private List<VehicleImage> images = new ArrayList<>();

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

    public void update(String brand, String model, Integer year, VehicleCategory category,
                        Transmission transmission, Integer seats, Boolean airConditioning,
                        BigDecimal pricePerDay, String currency, Integer unitsCount,
                        List<String> pickupLocations) {
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

    public void addImage(String url, String caption, Integer displayOrder, boolean isPrimary) {
        VehicleImage image = new VehicleImage(this, url, caption, displayOrder, isPrimary);
        if (isPrimary) {
            this.images.forEach(img -> img.setPrimary(false));
            this.coverImageUrl = url;
        }
        this.images.add(image);
    }

    public void removeImage(String imageId) {
        this.images.removeIf(img -> img.getId().equals(imageId));
    }

    public void setPrimaryImage(String imageId) {
        this.images.stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .ifPresent(target -> {
                    this.images.forEach(img -> img.setPrimary(img.getId().equals(imageId)));
                    this.coverImageUrl = target.getUrl();
                });
    }

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
    public String getCoverImageUrl() { return coverImageUrl; }
    public List<VehicleImage> getImages() { return images; }
}
