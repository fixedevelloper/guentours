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

    /** Redacts the card PAN/CVV/expiry so an accidental log line never leaks them. */
    @Override
    public String toString() {
        return "ChargeRequest[amount=%s, currency=%s, countryCode=%s, countryCurrency=%s, paymentMethod=%s, cardNumber=%s, cardHolderName=%s, expiry=REDACTED, cvv=REDACTED, mobileNumber=%s, customerEmail=%s, paymentReference=%s, customerIp=%s, billingAddress=%s]"
                .formatted(amount, currency, countryCode, countryCurrency, paymentMethod,
                        maskCardNumber(cardNumber), cardHolderName, mobileNumber, customerEmail,
                        paymentReference, customerIp, billingAddress);
    }

    private static String maskCardNumber(String cardNumber) {
        if (cardNumber == null) {
            return null;
        }
        return cardNumber.length() >= 4
                ? "*".repeat(cardNumber.length() - 4) + cardNumber.substring(cardNumber.length() - 4)
                : "*".repeat(cardNumber.length());
    }
}