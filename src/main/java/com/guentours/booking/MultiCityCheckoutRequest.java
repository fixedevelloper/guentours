package com.guentours.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MultiCityCheckoutRequest(
        /** One offer id per leg, in itinerary order - as returned by a single {@code MultiCityItinerary}. */
        @NotEmpty @Size(min = 2, max = 6) List<String> legOfferIds,
        @NotBlank @Email String contactEmail,
        @NotBlank String contactFullName,
        String contactPhone,
        @NotEmpty @Valid List<TravelerRequest> travelers,
        /** Defaults to {@link PaymentPlan#PAY_NOW} when omitted. */
        PaymentPlan paymentPlan
) {
}
