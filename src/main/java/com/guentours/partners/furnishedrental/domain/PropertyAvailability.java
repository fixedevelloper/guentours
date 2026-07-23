package com.guentours.partners.furnishedrental.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "property_availabilities", uniqueConstraints =
    @UniqueConstraint(columnNames = {"property_id", "stay_date"}))
public class PropertyAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(nullable = false)
    private LocalDate stayDate;

    @Column(nullable = false)
    private Boolean isAvailable = true;

    private BigDecimal priceOverride;

    protected PropertyAvailability() {}

    public PropertyAvailability(Property property, LocalDate stayDate, Boolean isAvailable) {
        this.property = property;
        this.stayDate = stayDate;
        this.isAvailable = isAvailable;
    }

    public String getId() { return id; }
    public LocalDate getStayDate() { return stayDate; }
    public Boolean getIsAvailable() { return isAvailable; }
    public BigDecimal getPriceOverride() { return priceOverride; }
}
