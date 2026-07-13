package com.guentours.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CheckoutRequest(
        @NotBlank String offerId,
        @NotNull OfferType offerType,
        @NotBlank @Email String contactEmail,
        @NotBlank String contactFullName,
        String contactPhone,
        @NotEmpty @Valid List<TravelerRequest> travelers,
        /** Defaults to {@link PaymentPlan#PAY_NOW} when omitted. */
        PaymentPlan paymentPlan
) {
}
