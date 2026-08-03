package com.guentours.shared;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Computes the booking fee GuenTours adds on top of the provider's own price, as a percentage of
 * that price (e.g. 0.15 = 15%) rather than a flat amount - so the fee scales with the size of the
 * offer instead of being a fixed number regardless of price. The fee is never deducted from what
 * the provider is paid - it is added on top of every displayed price and charged to the customer
 * alongside it, then tracked separately in the commission wallet.
 */
@Component
public class CommissionPolicy {

    private final BigDecimal flightFeeRate;
    private final BigDecimal hotelFeeRate;
    private final BigDecimal vehicleFeeRate;
    private final BigDecimal propertyFeeRate;

    public CommissionPolicy(@Value("${app.commission.flight-fee-rate:0.15}") BigDecimal flightFeeRate,
                             @Value("${app.commission.hotel-fee-rate:0.15}") BigDecimal hotelFeeRate,
                             @Value("${app.commission.vehicle-fee-rate:0.15}") BigDecimal vehicleFeeRate,
                             @Value("${app.commission.property-fee-rate:0.15}") BigDecimal propertyFeeRate) {
        this.flightFeeRate = flightFeeRate;
        this.hotelFeeRate = hotelFeeRate;
        this.vehicleFeeRate = vehicleFeeRate;
        this.propertyFeeRate = propertyFeeRate;
    }

    /** Adds one flight fee to a provider price - once per flight segment (leg). */
    public Money addFlightFee(Money providerPrice) {
        return addFee(providerPrice, flightFeeRate);
    }

    /** Adds one hotel fee to a provider price - once per room booked. */
    public Money addHotelFee(Money providerPrice) {
        return addFee(providerPrice, hotelFeeRate);
    }

    /** Adds one vehicle fee to a provider price - once per rental booked. */
    public Money addVehicleFee(Money providerPrice) {
        return addFee(providerPrice, vehicleFeeRate);
    }

    /** Adds one property fee to a provider price - once per stay booked. */
    public Money addPropertyFee(Money providerPrice) {
        return addFee(providerPrice, propertyFeeRate);
    }

    private Money addFee(Money providerPrice, BigDecimal rate) {
        return providerPrice.add(new Money(providerPrice.amount().multiply(rate), providerPrice.currency()));
    }

    /**
     * Recovers the fee portion already folded into a total price produced by {@link #addFlightFee}:
     * since total = providerPrice * (1 + rate), fee = total * rate / (1 + rate). Lets the commission
     * wallet record the fee amount from a booking's final (already fee-inclusive) price alone,
     * without needing to keep the original pre-fee provider price around.
     */
    public Money flightFeeFromTotal(Money totalWithFee) {
        return feeFromTotal(totalWithFee, flightFeeRate);
    }

    public Money hotelFeeFromTotal(Money totalWithFee) {
        return feeFromTotal(totalWithFee, hotelFeeRate);
    }

    public Money vehicleFeeFromTotal(Money totalWithFee) {
        return feeFromTotal(totalWithFee, vehicleFeeRate);
    }

    public Money propertyFeeFromTotal(Money totalWithFee) {
        return feeFromTotal(totalWithFee, propertyFeeRate);
    }

    private Money feeFromTotal(Money totalWithFee, BigDecimal rate) {
        BigDecimal factor = rate.divide(BigDecimal.ONE.add(rate), 10, RoundingMode.HALF_UP);
        return new Money(totalWithFee.amount().multiply(factor), totalWithFee.currency());
    }
}
