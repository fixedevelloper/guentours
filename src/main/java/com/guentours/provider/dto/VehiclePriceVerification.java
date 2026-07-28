package com.guentours.provider.dto;

import com.guentours.shared.Money;

public record VehiclePriceVerification(Money currentPrice, boolean available) {
    public boolean priceChanged(Money quotedPrice) {
        return !currentPrice.equals(quotedPrice);
    }
}