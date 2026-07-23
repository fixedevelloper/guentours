package com.guentours.partners.hotel.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "room_availabilities", uniqueConstraints =
    @UniqueConstraint(columnNames = {"room_type_id", "stay_date"}))
public class RoomAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @Column(nullable = false)
    private LocalDate stayDate;

    @Column(nullable = false)
    private Integer roomsAvailable;

    private BigDecimal priceOverride;

    protected RoomAvailability() {}

    public RoomAvailability(RoomType roomType, LocalDate stayDate, Integer roomsAvailable) {
        this.roomType = roomType;
        this.stayDate = stayDate;
        this.roomsAvailable = roomsAvailable;
    }

    public void decrementRooms(int qty) {
        if (roomsAvailable < qty) throw new IllegalStateException("Chambres insuffisantes");
        roomsAvailable -= qty;
    }

    public String getId() { return id; }
    public RoomType getRoomType() { return roomType; }
    public LocalDate getStayDate() { return stayDate; }
    public Integer getRoomsAvailable() { return roomsAvailable; }
    public BigDecimal getPriceOverride() { return priceOverride; }
}
