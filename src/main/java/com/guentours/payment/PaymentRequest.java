package com.guentours.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Card fields are required only when {@code paymentMethod == CARD}; {@code mobileNumber} is
 * required for the mobile-money methods. Cross-field validation happens in
 * {@link PaymentService} since it depends on which method was chosen.
 */
public record PaymentRequest(
        @NotBlank String bookingId,
        @NotNull PaymentMethod paymentMethod,
        String cardNumber,
        String cardHolderName,
        String expiry,
        String cvv,
        String mobileNumber
) {
}
