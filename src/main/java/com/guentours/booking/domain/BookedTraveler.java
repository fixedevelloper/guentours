package com.guentours.booking.domain;

import com.guentours.provider.PassengerType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDate;

@Embeddable
public class BookedTraveler {

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "passport_number")
    private String passportNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "passenger_type")
    private PassengerType type;

    /** Seat picked at the seat-selection checkout step (FLIGHT only); null if none was assigned. */
    @Column(name = "seat_number")
    private String seatNumber;

    /** ISO 3166-1 alpha-2 nationality; required by some providers' flight booking APIs (e.g. Travelopro). */
    @Column(name = "nationality")
    private String nationality;

    /** ISO 3166-1 alpha-2 passport-issuing country; optional, requested by some flight booking APIs. */
    @Column(name = "passport_issue_country")
    private String passportIssueCountry;

    /** Passport expiry date; optional, requested by some flight booking APIs. */
    @Column(name = "passport_expiry_date")
    private LocalDate passportExpiryDate;

    protected BookedTraveler() {
        // JPA
    }

    public BookedTraveler(String fullName, LocalDate dateOfBirth, String passportNumber, PassengerType type,
                           String seatNumber, String nationality, String passportIssueCountry,
                           LocalDate passportExpiryDate) {
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.passportNumber = passportNumber;
        this.type = type;
        this.seatNumber = seatNumber;
        this.nationality = nationality;
        this.passportIssueCountry = passportIssueCountry;
        this.passportExpiryDate = passportExpiryDate;
    }

    public String getFullName() {
        return fullName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getPassportNumber() {
        return passportNumber;
    }

    public PassengerType getType() {
        return type;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public String getNationality() {
        return nationality;
    }

    public String getPassportIssueCountry() {
        return passportIssueCountry;
    }

    public LocalDate getPassportExpiryDate() {
        return passportExpiryDate;
    }
}
