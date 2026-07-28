package com.guentours.payment.gateway.flutterwave;

import com.flutterwave.bean.GooglePayRequest;
import com.flutterwave.bean.Response;
import com.flutterwave.services.GooglePay;
import com.guentours.payment.gateway.ChargeRequest;
import com.guentours.payment.gateway.ChargeResult;
import com.guentours.payment.gateway.ChargeStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class FlutterwaveGooglePayGateway {

    ChargeResult charge(ChargeRequest request, String paymentReference) {
        if (request.billingAddress() == null) {
            return new ChargeResult(ChargeStatus.FAILED, paymentReference,
                    request.payerReferenceLast4(), "Adresse de facturation requise pour Google Pay", null);
        }

        Response response;
        try {
            response = new GooglePay().runTransaction(new GooglePayRequest(
                    paymentReference, request.amount(), request.customerEmail(), request.mobileNumber(),
                    request.currency(), request.customerIp(),
                    null, // ⚠️ TOKEN GOOGLE PAY MANQUANT — voir note ci-dessous
                    request.billingAddress().zipCode(), request.billingAddress().city(),
                    request.billingAddress().address(), request.billingAddress().state(),
                    request.billingAddress().countryCode(), "Paiement GuenTours", null));
        } catch (Exception e) {
            log.error("Erreur Flutterwave Google Pay pour tx_ref {}", paymentReference, e);
            return new ChargeResult(ChargeStatus.FAILED, paymentReference,
                    request.payerReferenceLast4(), "Erreur de communication avec le gateway", null);
        }

        return mapResponse(response, paymentReference, request.payerReferenceLast4());
    }

    private ChargeResult mapResponse(Response response, String paymentReference, String last4) {
        if (response == null) {
            return new ChargeResult(ChargeStatus.FAILED, paymentReference, last4, "Réponse gateway vide", null);
        }
        if ("error".equalsIgnoreCase(response.getStatus())) {
            return new ChargeResult(ChargeStatus.FAILED, paymentReference, last4, response.getMessage(), null);
        }
        // Google/Apple Pay répondent en général avec une redirection ou une confirmation immédiate ; à ce
        // stade on ne connaît pas encore l'issue finale tant que confirmée par webhook.
        return new ChargeResult(ChargeStatus.PENDING, paymentReference, last4, null, null);
    }
}