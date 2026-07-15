package com.guentours.shared;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Computes the fixed booking fee GuenTours adds on top of the provider's own price. The fee is
 * never deducted from what the provider is paid - it is added on top of every displayed price
 * and charged to the customer alongside it, then tracked separately in the commission wallet.
 */
@Component
public class CommissionPolicy {

    private final BigDecimal flightFeeAmount;
    private final BigDecimal hotelFeeAmount;

    public CommissionPolicy(@Value("${app.commission.flight-fee-amount:15}") BigDecimal flightFeeAmount,
                             @Value("${app.commission.hotel-fee-amount:15}") BigDecimal hotelFeeAmount) {
        this.flightFeeAmount = flightFeeAmount;
        this.hotelFeeAmount = hotelFeeAmount;
    }

    public Money flightFee(String currency) {
        return new Money(flightFeeAmount, currency);
    }

    public Money hotelFee(String currency) {
        return new Money(hotelFeeAmount, currency);
    }

    /** Adds one flight fee to a provider price - once per flight segment (leg). */
    public Money addFlightFee(Money providerPrice) {
        return providerPrice.add(flightFee(providerPrice.currency()));
    }

    /** Adds one hotel fee to a provider price - once per room booked. */
    public Money addHotelFee(Money providerPrice) {
        return providerPrice.add(hotelFee(providerPrice.currency()));
    }
}
