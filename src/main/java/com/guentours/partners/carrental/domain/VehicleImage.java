package com.guentours.partners.carrental.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_images")
public class VehicleImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    @JsonIgnore // Évite la boucle de sérialisation circulaire VehicleImage -> Vehicle -> VehicleImage
    private Vehicle vehicle;

    @Column(nullable = false, length = 1024)
    private String url;

    private String caption;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected VehicleImage() {}

    public VehicleImage(Vehicle vehicle, String url, String caption, Integer displayOrder, boolean primary) {
        this.vehicle = vehicle;
        this.url = url;
        this.caption = caption;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
        this.primary = primary;
    }

    public String getId() { return id; }
    public Vehicle getVehicle() { return vehicle; }
    public String getUrl() { return url; }
    public String getCaption() { return caption; }
    public Integer getDisplayOrder() { return displayOrder; }
    public boolean isPrimary() { return primary; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setPrimary(boolean primary) { this.primary = primary; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
}
