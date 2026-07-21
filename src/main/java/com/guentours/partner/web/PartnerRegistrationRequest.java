package com.guentours.partner.web;
import com.guentours.partner.domain.PartnerType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PartnerRegistrationRequest(
        @NotNull PartnerType partnerType,
        @NotBlank String companyName,
        @NotBlank String registrationNumber,
        @NotBlank String contactName,
        @NotBlank @Email String email,
        @NotBlank String phone,
        @NotBlank String city,
        @NotBlank String country,
        Integer fleetOrUnitsCount,
        String description
) {}
