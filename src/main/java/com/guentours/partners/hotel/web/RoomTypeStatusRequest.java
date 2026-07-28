package com.guentours.partners.hotel.web;

import com.guentours.partners.hotel.domain.ListingStatus;
import jakarta.validation.constraints.NotNull;

public record RoomTypeStatusRequest(
        @NotNull ListingStatus status
) {}
