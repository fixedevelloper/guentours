package com.guentours.partners.hotel.domain;
import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hotels")
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String partnerId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String country;

    private Integer starRating;

    @Column(length = 2000)
    private String description;

    @Column(length = 1024)
    private String coverImageUrl;

    @ElementCollection
    @CollectionTable(name = "hotel_amenities", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "amenity")
    private List<String> amenities = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "hotel_id")
    @OrderBy("displayOrder ASC")
    private List<HotelImage> images = new ArrayList<>();

    private LocalTime checkInTime;
    private LocalTime checkOutTime;

    @Enumerated(EnumType.STRING)
    private ListingStatus status = ListingStatus.ACTIVE;

    protected Hotel() {}

    public Hotel(String partnerId, String name, String address, String city, String country,
                 Integer starRating, String description, String coverImageUrl, List<String> amenities,
                 LocalTime checkInTime, LocalTime checkOutTime) {
        this.partnerId = partnerId;
        this.name = name;
        this.address = address;
        this.city = city;
        this.country = country;
        this.starRating = starRating;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.amenities = amenities != null ? amenities : new ArrayList<>();
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
    }

    // --- Méthodes métier pour la gestion des images ---

    public void addImage(String url, String caption, Integer displayOrder, boolean isPrimary) {
        HotelImage image = new HotelImage(url, caption, displayOrder, isPrimary);
        if (isPrimary) {
            // S'assurer qu'une seule image est marquée comme principale
            this.images.forEach(img -> img.setPrimary(false));
            this.coverImageUrl = url; // Mettre à jour l'image de couverture principale
        }
        this.images.add(image);
    }

    public void removeImage(String imageId) {
        this.images.removeIf(img -> img.getId().equals(imageId));
    }

    public void updateCoverImage(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public void suspend() { this.status = ListingStatus.SUSPENDED; }
    public void activate() { this.status = ListingStatus.ACTIVE; }

    // --- Getters ---

    public String getId() { return id; }
    public String getPartnerId() { return partnerId; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getCountry() { return country; }
    public Integer getStarRating() { return starRating; }
    public String getDescription() { return description; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public List<String> getAmenities() { return amenities; }
    public List<HotelImage> getImages() { return images; }
    public LocalTime getCheckInTime() { return checkInTime; }
    public LocalTime getCheckOutTime() { return checkOutTime; }
    public ListingStatus getStatus() { return status; }
}