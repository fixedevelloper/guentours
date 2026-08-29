package com.guentours.provider;

import java.time.LocalDate;
import java.util.List;

public record PassengerInfo(String fullName, LocalDate dateOfBirth, String passportNumber, PassengerType type,
                            String nationality, String passportIssueCountry, LocalDate passportExpiryDate,
                            List<SelectedAncillary> selectedAncillaries) {
    public PassengerInfo {
        selectedAncillaries = selectedAncillaries == null ? List.of() : List.copyOf(selectedAncillaries);
    }
}
