package com.guentours.payment.web;

import com.guentours.payment.domain.PaymentMethod;
import com.guentours.payment.gateway.BillingAddress;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Card fields are required only when {@code paymentMethod == CARD}; {@code mobileNumber} is
 * required for MOBILE_MONEY; {@code billingAddress} is required for GOOGLE_PAY/APPLE_PAY/PAYPAL.
 * Cross-field validation happens in {@link com.guentours.payment.service.PaymentService} since it
 * depends on which method was chosen.
 *
 * {@code customerIp} n'est PAS rempli par le client : le controller l'écrase toujours avec
 * l'adresse IP réelle extraite de la requête HTTP, pour éviter qu'un client falsifie cette valeur.
 */
public record PaymentRequest(
        @NotBlank String bookingId,
        @NotNull PaymentMethod paymentMethod,
        @NotBlank @Size(min = 2, max = 2) String countryCode,
        @NotBlank @Size(min = 3, max = 3) String countryCurrency,
        String cardNumber,
        String cardHolderName,
        String expiry,
        String cvv,
        String mobileNumber,
        BillingAddress billingAddress,
        String customerIp // renseigné côté serveur uniquement, voir PaymentController
) {
    /** Constructeur pratique pour le binding JSON du frontend : l'IP n'est jamais fournie par le client. */
    public static PaymentRequest fromClientPayload(String bookingId, PaymentMethod paymentMethod,
                                                   String countryCode, String countryCurrency, String cardNumber, String cardHolderName,
                                                   String expiry, String cvv, String mobileNumber, BillingAddress billingAddress) {
        return new PaymentRequest(bookingId, paymentMethod, countryCode, countryCurrency, cardNumber,
                cardHolderName, expiry, cvv, mobileNumber, billingAddress, null);
    }

    /** Retourne une copie avec l'IP serveur injectée, à utiliser juste avant PaymentService.pay(...). */
    public PaymentRequest withCustomerIp(String resolvedIp) {
        return new PaymentRequest(bookingId, paymentMethod, countryCode, countryCurrency, cardNumber,
                cardHolderName, expiry, cvv, mobileNumber, billingAddress, resolvedIp);
    }
}