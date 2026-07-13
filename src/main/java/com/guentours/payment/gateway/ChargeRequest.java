package com.guentours.payment.gateway;

import com.guentours.payment.PaymentMethod;
import com.guentours.shared.Money;

public record ChargeRequest(Money amount, PaymentMethod paymentMethod, String cardNumber, String cardHolderName,
                             String expiry, String cvv, String mobileNumber) {

    /** Last 4 digits of whichever identifier is relevant to the chosen method - never the full PAN/number. */
    public String payerReferenceLast4() {
        String reference = paymentMethod == PaymentMethod.CARD ? cardNumber : mobileNumber;
        if (reference == null) {
            return null;
        }
        return reference.length() >= 4 ? reference.substring(reference.length() - 4) : reference;
    }
}
