package com.guentours.booking.web;

import com.guentours.booking.domain.OfferType;
import com.guentours.provider.PassengerType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Quotes the priced extras (baggage/meal/seat/insurance) available for an offer, ahead of the
 * final checkout submission - backs the "additional options" step. {@code travelers} only needs
 * basic demographics: some providers (Travel Terminus) require at least names/types to price
 * ancillaries at all, but full traveler details (passport, etc.) aren't needed at this stage.
 */
public record AncillaryOptionsRequest(
        @NotBlank String offerId,
        @NotNull OfferType offerType,
        @Valid List<PassengerBasicInfo> travelers
) {
    public record PassengerBasicInfo(@NotBlank String fullName, @NotNull PassengerType type) {
    }
}
