package com.guentours.partners.web;

public record PartnerUpdateRequest(
        String companyName,
        String phone,
        String city,
        String country,
        String description
) {}