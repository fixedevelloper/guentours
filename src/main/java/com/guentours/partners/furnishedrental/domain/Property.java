package com.guentours.partners.furnishedrental.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "properties")
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String partnerId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PropertyType propertyType;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private Integer bedrooms;

    @Column(nullable = false)
    private Integer bathrooms;

    @Column(nullable = false)
    private Integer maxGuests;

    @ElementCollection
    @CollectionTable(name = "property_amenities", joinColumns = @JoinColumn(name = "property_id"))
    @Column(name = "amenity")
    private List<String> amenities;

    @Column(nullable = false)
    private BigDecimal pricePerNight;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private Integer minStayNights;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    private ListingStatus status = ListingStatus.ACTIVE;

    protected Property() {}

    public Property(String partnerId, String title, PropertyType propertyType, String address,
                     String city, String country, Integer bedrooms, Integer bathrooms,
                     Integer maxGuests, List<String> amenities, BigDecimal pricePerNight,
                     String currency, Integer minStayNights, String description) {
        this.partnerId = partnerId;
        this.title = title;
        this.propertyType = propertyType;
        this.address = address;
        this.city = city;
        this.country = country;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.maxGuests = maxGuests;
        this.amenities = amenities;
        this.pricePerNight = pricePerNight;
        this.currency = currency;
        this.minStayNights = minStayNights;
        this.description = description;
    }

    public void suspend() { this.status = ListingStatus.SUSPENDED; }
    public void activate() { this.status = ListingStatus.ACTIVE; }

    public String getId() { return id; }
    public String getPartnerId() { return partnerId; }
    public String getTitle() { return title; }
    public PropertyType getPropertyType() { return propertyType; }
    public String getCity() { return city; }
    public String getCountry() { return country; }
    public Integer getBedrooms() { return bedrooms; }
    public Integer getBathrooms() { return bathrooms; }
    public Integer getMaxGuests() { return maxGuests; }
    public BigDecimal getPricePerNight() { return pricePerNight; }
    public Integer getMinStayNights() { return minStayNights; }
    public ListingStatus getStatus() { return status; }
}
