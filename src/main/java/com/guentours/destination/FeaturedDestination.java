package com.guentours.destination;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "featured_destinations")
public class FeaturedDestination {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "city_name", nullable = false, length = 150)
    private String cityName;

    @Column(name = "country_name", nullable = false, length = 150)
    private String countryName;

    /** IATA airport/city code used to deep-link the homepage card into a real flight search - null
     *  for a destination an admin added by hand without one. */
    @Column(name = "destination_code", length = 10)
    private String destinationCode;

    @Column(name = "image_url", length = 1024)
    private String imageUrl;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected FeaturedDestination() {
        // JPA
    }

    public FeaturedDestination(String cityName, String countryName, String destinationCode, String imageUrl,
                                int displayOrder, boolean active) {
        this.cityName = cityName;
        this.countryName = countryName;
        this.destinationCode = destinationCode;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
        this.active = active;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void update(String cityName, String countryName, String destinationCode, String imageUrl,
                        int displayOrder, boolean active) {
        this.cityName = cityName;
        this.countryName = countryName;
        this.destinationCode = destinationCode;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public String getCityName() {
        return cityName;
    }

    public String getCountryName() {
        return countryName;
    }

    public String getDestinationCode() {
        return destinationCode;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
