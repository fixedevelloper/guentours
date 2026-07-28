package com.guentours.payment.gateway;

import com.guentours.payment.domain.PaymentMethod;

import java.math.BigDecimal;

public record ChargeRequest(
        BigDecimal amount,
        String currency,          // Money.currency du booking (montant réellement dû)
        String countryCode,       // pays choisi par le payeur (ISO2)
        String countryCurrency,   // devise du pays choisi par le payeur
        PaymentMethod paymentMethod,
        String cardNumber,
        String cardHolderName,
        String expiry,
        String cvv,
        String mobileNumber,
        String customerEmail,
        String paymentReference,  // = payment.getId(), utilisé comme tx_ref partout
        String customerIp,
        BillingAddress billingAddress
) {
    public String payerReferenceLast4() {
        return paymentMethod == PaymentMethod.CARD && cardNumber != null && cardNumber.length() >= 4
                ? cardNumber.substring(cardNumber.length() - 4)
                : mobileNumber;
    }
}