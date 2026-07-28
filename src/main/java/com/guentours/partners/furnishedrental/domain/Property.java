package com.guentours.partners.furnishedrental.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
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

    @Column(name = "cover_image_url", length = 1024)
    private String coverImageUrl;

    // Eager : PropertyResponse.from() lit cette collection dans le contrôleur, après la fermeture
    // de la session Hibernate (open-in-view=false) - un fetch lazy y lèverait LazyInitializationException.
    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("displayOrder ASC")
    private List<PropertyImage> images = new ArrayList<>();

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

    public void update(String title, PropertyType propertyType, String address, String city, String country,
                        Integer bedrooms, Integer bathrooms, Integer maxGuests, List<String> amenities,
                        BigDecimal pricePerNight, String currency, Integer minStayNights, String description) {
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

    public void addImage(String url, String caption, Integer displayOrder, boolean isPrimary) {
        PropertyImage image = new PropertyImage(this, url, caption, displayOrder, isPrimary);
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
    public String getTitle() { return title; }
    public PropertyType getPropertyType() { return propertyType; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getCountry() { return country; }
    public Integer getBedrooms() { return bedrooms; }
    public Integer getBathrooms() { return bathrooms; }
    public Integer getMaxGuests() { return maxGuests; }
    public List<String> getAmenities() { return amenities; }
    public BigDecimal getPricePerNight() { return pricePerNight; }
    public Integer getMinStayNights() { return minStayNights; }
    public String getDescription() { return description; }
    public ListingStatus getStatus() { return status; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public List<PropertyImage> getImages() { return images; }

    public String getCurrency() {
        return currency;
    }
}
