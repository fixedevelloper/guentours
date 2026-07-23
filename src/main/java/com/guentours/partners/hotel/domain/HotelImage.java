package com.guentours.partners.hotel.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hotel_images")
public class HotelImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

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

    public HotelImage(String url, String caption, Integer displayOrder, boolean primary) {
        this.url = url;
        this.caption = caption;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.primary = primary;
    }

    // Getters et Setters / Méthodes métier
    public String getId() { return id; }
    public String getUrl() { return url; }
    public String getCaption() { return caption; }
    public Integer getDisplayOrder() { return displayOrder; }
    public boolean isPrimary() { return primary; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setPrimary(boolean primary) { this.primary = primary; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}
