package com.guentours.geo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** One row per city that hotels can be searched in, with the coordinates needed to place it on a map. */
@Entity
@Table(
        name = "hotel_cities",
        indexes = @Index(name = "idx_hotel_cities_name", columnList = "city_name"),
        uniqueConstraints = @UniqueConstraint(name = "uk_hotel_cities_name_country", columnNames = {"city_name", "country_name"})
)
public class HotelCity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city_name", nullable = false)
    private String cityName;

    @Column(name = "country_name", nullable = false)
    private String countryName;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    protected HotelCity() {
        // JPA
    }

    public HotelCity(String cityName, String countryName, Double latitude, Double longitude) {
        this.cityName = cityName;
        this.countryName = countryName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void refresh(String cityName, String countryName, Double latitude, Double longitude) {
        this.cityName = cityName;
        this.countryName = countryName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getId() {
        return id;
    }

    public String getCityName() {
        return cityName;
    }

    public String getCountryName() {
        return countryName;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
}
