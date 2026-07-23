package com.guentours.partners.hotel.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "room_types")
public class RoomType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    @JsonIgnore // Évite la boucle de sérialisation circulaire RoomType -> Hotel -> RoomType
    private Hotel hotel;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false)
    private Integer maxAdults;

    @Column(nullable = false)
    private Integer maxChildren;

    private String bedType;
    private Double sizeSqm;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private Integer totalRooms;

    @Column(length = 1024)
    private String coverImageUrl;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "room_type_id")
    @OrderBy("displayOrder ASC")
    private List<RoomTypeImage> images = new ArrayList<>();

    protected RoomType() {}

    public RoomType(Hotel hotel, String name, Integer maxAdults, Integer maxChildren,
                    String bedType, Double sizeSqm, BigDecimal basePrice, String currency,
                    Integer totalRooms, String coverImageUrl) {
        this.hotel = hotel;
        this.name = name;
        this.maxAdults = maxAdults;
        this.maxChildren = maxChildren;
        this.bedType = bedType;
        this.sizeSqm = sizeSqm;
        this.basePrice = basePrice;
        this.currency = currency;
        this.totalRooms = totalRooms;
        this.coverImageUrl = coverImageUrl;
    }

    // --- Gestion des images de la chambre ---

    public void addImage(String url, String caption, Integer displayOrder, boolean isPrimary) {
        RoomTypeImage image = new RoomTypeImage(url, caption, displayOrder, isPrimary);
        if (isPrimary) {
            this.images.forEach(img -> img.setPrimary(false));
            this.coverImageUrl = url;
        }
        this.images.add(image);
    }

    public void removeImage(String imageId) {
        this.images.removeIf(img -> img.getId().equals(imageId));
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    // --- Getters ---

    public String getId() { return id; }
    public Hotel getHotel() { return hotel; }
    public String getName() { return name; }
    public Integer getMaxAdults() { return maxAdults; }
    public Integer getMaxChildren() { return maxChildren; }
    public String getBedType() { return bedType; }
    public Double getSizeSqm() { return sizeSqm; }
    public BigDecimal getBasePrice() { return basePrice; }
    public String getCurrency() { return currency; }
    public Integer getTotalRooms() { return totalRooms; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public List<RoomTypeImage> getImages() { return images; }
}