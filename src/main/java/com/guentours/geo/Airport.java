package com.guentours.geo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * One row per IATA airport code. The code itself is the primary key, which gives us the
 * "unique et indexé pour des recherches ultra-rapides" lookup for free (primary key index)
 * on top of the extra index on {@code city} for the autocomplete search.
 */
@Entity
@Table(
        name = "airports",
        indexes = {
                @Index(name = "idx_airports_city", columnList = "city"),
                @Index(name = "idx_airports_name", columnList = "airport_name")
        }
)
public class Airport {

    @Id
    @Column(name = "airport_code", length = 3, nullable = false, updatable = false)
    private String airportCode;

    @Column(name = "airport_name", nullable = false)
    private String airportName;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String country;

    protected Airport() {
        // JPA
    }

    public Airport(String airportCode, String airportName, String city, String country) {
        this.airportCode = airportCode;
        this.airportName = airportName;
        this.city = city;
        this.country = country;
    }

    public String getAirportCode() {
        return airportCode;
    }

    public String getAirportName() {
        return airportName;
    }

    public String getCity() {
        return city;
    }

    public String getCountry() {
        return country;
    }
}
