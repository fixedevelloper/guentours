package com.guentours.shared;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the percentage-based fee behavior: the fee is a fraction of the provider's price (e.g. 0.15
 * = 15%), and {@code *FeeFromTotal} must recover exactly the fee that {@code addXFee} folded in,
 * from the final total alone - this is what {@code CommissionEventListener} relies on to record the
 * commission wallet entry without keeping the original pre-fee provider price around.
 */
class CommissionPolicyTest {

    private final CommissionPolicy policy = new CommissionPolicy(
            BigDecimal.valueOf(0.15), BigDecimal.valueOf(0.15), BigDecimal.valueOf(0.15), BigDecimal.valueOf(0.15));

    @Test
    void addsAPercentageOfTheProviderPriceRatherThanAFlatAmount() {
        Money providerPrice = new Money(BigDecimal.valueOf(400), "EUR");

        assertThat(policy.addFlightFee(providerPrice).amount()).isEqualByComparingTo("460.00");
        assertThat(policy.addHotelFee(providerPrice).amount()).isEqualByComparingTo("460.00");
        assertThat(policy.addVehicleFee(providerPrice).amount()).isEqualByComparingTo("460.00");
        assertThat(policy.addPropertyFee(providerPrice).amount()).isEqualByComparingTo("460.00");
    }

    @Test
    void feeScalesWithPriceUnlikeAFlatFee() {
        Money oneRoom = new Money(BigDecimal.valueOf(100), "EUR");
        Money threeRooms = new Money(BigDecimal.valueOf(300), "EUR");

        Money oneRoomTotal = policy.addHotelFee(oneRoom);
        Money threeRoomsTotal = policy.addHotelFee(threeRooms);

        assertThat(threeRoomsTotal.amount()).isEqualByComparingTo(oneRoomTotal.amount().multiply(BigDecimal.valueOf(3)));
    }

    @Test
    void feeFromTotalRecoversExactlyTheFeeThatWasAdded() {
        Money providerPrice = new Money(BigDecimal.valueOf(400), "EUR");
        Money totalWithFee = policy.addFlightFee(providerPrice);

        Money recoveredFee = policy.flightFeeFromTotal(totalWithFee);

        assertThat(recoveredFee.amount()).isEqualByComparingTo("60.00");
        assertThat(totalWithFee.subtract(recoveredFee).amount()).isEqualByComparingTo(providerPrice.amount());
    }
}
