package com.guentours.provider;

import java.time.LocalDate;

public record PassengerInfo(String fullName, LocalDate dateOfBirth, String passportNumber, PassengerType type) {
}
