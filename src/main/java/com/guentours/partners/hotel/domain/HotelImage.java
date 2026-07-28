package com.guentours.partners.hotel.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hotel_images")
public class HotelImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    @JsonIgnore // Évite la boucle de sérialisation circulaire HotelImage -> Hotel -> HotelImage
    private Hotel hotel;

    @Column(nullable = false, length = 1024)
    private String url;

    private String caption;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected HotelImage() {}

    public HotelImage(Hotel hotel, String url, String caption, Integer displayOrder, boolean primary) {
        this.hotel = hotel;
        this.url = url;
        this.caption = caption;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.primary = primary;
    }

    // Getters et Setters / Méthodes métier
    public String getId() { return id; }
    public Hotel getHotel() { return hotel; }
    public String getUrl() { return url; }
    public String getCaption() { return caption; }
    public Integer getDisplayOrder() { return displayOrder; }
    public boolean isPrimary() { return primary; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setPrimary(boolean primary) { this.primary = primary; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}
