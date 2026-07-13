package com.guentours.booking;

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

    protected BookedTraveler() {
        // JPA
    }

    public BookedTraveler(String fullName, LocalDate dateOfBirth, String passportNumber, PassengerType type) {
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.passportNumber = passportNumber;
        this.type = type;
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
}
