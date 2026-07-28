package com.guentours.payment.gateway.flutterwave;

import com.flutterwave.bean.PaypalRequest;
import com.flutterwave.bean.Response;
import com.flutterwave.services.Paypal;
import com.guentours.payment.gateway.ChargeRequest;
import com.guentours.payment.gateway.ChargeResult;
import com.guentours.payment.gateway.ChargeStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class FlutterwavePaypalGateway {

    private final FlutterwaveProperties properties;

    FlutterwavePaypalGateway(FlutterwaveProperties properties) {
        this.properties = properties;
    }

    ChargeResult charge(ChargeRequest request, String paymentReference) {
        if (request.billingAddress() == null || request.cardHolderName() == null) {
            return new ChargeResult(ChargeStatus.FAILED, paymentReference,
                    request.payerReferenceLast4(), "Nom et adresse requis pour PayPal", null);
        }
        String[] nameParts = request.cardHolderName().split(" ", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        Response response;
        try {
            response = new Paypal().runTransaction(new PaypalRequest(
                    firstName, lastName,
                    request.billingAddress().address(), request.billingAddress().city(),
                    request.billingAddress().zipCode(), request.billingAddress().state(),
                    request.billingAddress().countryCode(),
                    paymentReference, request.amount(), request.customerEmail(), request.mobileNumber(),
                    request.currency(), request.customerIp(), null,
                    request.billingAddress().zipCode(), request.billingAddress().city(),
                    request.billingAddress().address(), request.billingAddress().state(),
                    request.billingAddress().countryCode(), properties.redirectUrl(), null));
        } catch (Exception e) {
            log.error("Erreur Flutterwave PayPal pour tx_ref {}", paymentReference, e);
            return new ChargeResult(ChargeStatus.FAILED, paymentReference,
                    request.payerReferenceLast4(), "Erreur de communication avec le gateway", null);
        }

        if (response == null) {
            return new ChargeResult(ChargeStatus.FAILED, paymentReference,
                    request.payerReferenceLast4(), "Réponse gateway vide", null);
        }
        if ("error".equalsIgnoreCase(response.getStatus())) {
            return new ChargeResult(ChargeStatus.FAILED, paymentReference,
                    request.payerReferenceLast4(), response.getMessage(), null);
        }
        // PayPal redirige systématiquement l'utilisateur — succès définitif seulement après retour/webhook.
        return new ChargeResult(ChargeStatus.PENDING, paymentReference, request.payerReferenceLast4(), null, null);
    }
}