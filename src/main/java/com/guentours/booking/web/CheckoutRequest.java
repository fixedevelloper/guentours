package com.guentours.booking.web;

import com.guentours.booking.domain.OfferType;
import com.guentours.booking.domain.PaymentPlan;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
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
        PaymentPlan paymentPlan,
        /**
         * Number of identical units to book, relevant ONLY when {@code offerType == OfferType.HOTEL}
         * (e.g. booking 2 identical rooms). Ignored for FLIGHT: ticket count/price are derived from
         * {@code travelers.size()} and each traveler's {@link com.guentours.provider.PassengerType},
         * not from this field. Defaults to 1 when omitted or null.
         */
        @Min(1) Integer quantity
) {
    public int quantityOrDefault() {
        return quantity == null ? 1 : quantity;
    }
}