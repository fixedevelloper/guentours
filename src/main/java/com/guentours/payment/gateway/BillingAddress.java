package com.guentours.payment.gateway;

public record BillingAddress(
        String zipCode,
        String city,
        String address,
        String state,
        String countryCode // ISO2
) {}