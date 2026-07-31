package com.guentours.payment.gateway.flutterwave;

import com.flutterwave.bean.Authorization;
import com.flutterwave.bean.CardRequest;
import com.flutterwave.bean.Response;
import com.flutterwave.services.CardCharge;
import com.guentours.payment.gateway.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class FlutterwaveCardGateway {

    private static final String STATUS_ERROR = "error";
    private static final String STATUS_SUCCESS = "success";

    private final FlutterwaveProperties properties;

    FlutterwaveCardGateway(FlutterwaveProperties properties) {
        this.properties = properties;
    }

    /** Étape 1 : initie la charge, retourne le mode d'authorization requis. */
    ChargeResult initiate(ChargeRequest request, String paymentReference) {

        String currency = request.currency() != null ? request.currency() : properties.defaultCurrency();

        CardRequest cardRequest = new CardRequest(
                request.cardNumber(),
                request.cvv(),
                request.expiry().split("/")[0],
                request.expiry().split("/")[1],
                currency,
                request.amount(),
                request.cardHolderName(),
                request.customerEmail(),
                paymentReference,
                properties.redirectUrl(),
                null
        );

        Response response;
        try {
            response = new CardCharge().runTransaction(cardRequest);
        } catch (Exception e) {
            log.error("Erreur lors de l'initiation carte Flutterwave pour tx_ref {}", paymentReference, e);
            return new ChargeResult(ChargeStatus.FAILED, paymentReference,
                    request.payerReferenceLast4(), "Erreur de communication avec le gateway", null);
        }

        if (response == null) {
            log.error("Réponse Flutterwave nulle pour tx_ref {}", paymentReference);
            return new ChargeResult(ChargeStatus.FAILED, paymentReference,
                    request.payerReferenceLast4(), "Réponse gateway vide", null);
        }

        log.info("Réponse Flutterwave carte (initiate) pour tx_ref {} : status={}, message={}",
                paymentReference, response.getStatus(), response.getMessage());

        if (STATUS_ERROR.equalsIgnoreCase(response.getStatus())) {
            log.warn("Initiation carte rejetée par Flutterwave pour tx_ref {} : {}",
                    paymentReference, response.getMessage());
            return new ChargeResult(ChargeStatus.FAILED, paymentReference,
                    request.payerReferenceLast4(), response.getMessage(), null);
        }

        if (!STATUS_SUCCESS.equalsIgnoreCase(response.getStatus())) {
            log.warn("Statut Flutterwave inattendu pour tx_ref {} (status={}), traité comme échec.",
                    paymentReference, response.getStatus());
            return new ChargeResult(ChargeStatus.FAILED, paymentReference,
                    request.payerReferenceLast4(),
                    "Statut gateway inattendu : " + response.getStatus(), null);
        }

        return resolveChargeOutcome(response, paymentReference, request.payerReferenceLast4());
    }

    /**
     * Une charge carte réussie sans aucune étape d'authorization (fréquent sur certains BIN, et
     * systématique sur le sandbox Flutterwave) ne porte que {@code data} - pas de {@code meta}.
     * Une charge qui exige un défi (PIN/AVS/REDIRECT/OTP) ne porte, à l'inverse, que
     * {@code meta.authorization} - pas de {@code data}. Ce n'est donc qu'en l'absence des deux que
     * la réponse est réellement de structure inattendue.
     */
    ChargeResult resolveChargeOutcome(Response response, String paymentReference, String payerReferenceLast4) {
        if (response.getData() != null && response.getData().getStatus() != null) {
            String flwStatus = response.getData().getStatus();
            return switch (flwStatus) {
                case "successful" -> new ChargeResult(ChargeStatus.SUCCEEDED, paymentReference,
                        payerReferenceLast4, null, null);
                case "pending" -> new ChargeResult(ChargeStatus.PENDING, paymentReference,
                        payerReferenceLast4, null, null);
                default -> new ChargeResult(ChargeStatus.FAILED, paymentReference, payerReferenceLast4,
                        "Paiement carte rejeté (" + flwStatus + ")", null);
            };
        }

        if (response.getMeta() != null && response.getMeta().getAuthorization() != null) {
            var authorization = response.getMeta().getAuthorization();
            return switch (authorization.getMode()) {
                case PIN -> new ChargeResult(ChargeStatus.PENDING_AUTHORIZATION, paymentReference,
                        payerReferenceLast4, null,
                        new AuthorizationChallenge(AuthorizationChallenge.AuthorizationType.PIN, null));
                case AUS_NOAUTH -> new ChargeResult(ChargeStatus.PENDING_AUTHORIZATION, paymentReference,
                        payerReferenceLast4, null,
                        new AuthorizationChallenge(AuthorizationChallenge.AuthorizationType.AVS, null));
                case REDIRECT -> new ChargeResult(ChargeStatus.PENDING_AUTHORIZATION, paymentReference,
                        payerReferenceLast4, null,
                        new AuthorizationChallenge(AuthorizationChallenge.AuthorizationType.REDIRECT,
                                authorization.getRedirect()));
                case OTP -> new ChargeResult(ChargeStatus.PENDING_AUTHORIZATION, paymentReference,
                        payerReferenceLast4, null,
                        new AuthorizationChallenge(AuthorizationChallenge.AuthorizationType.OTP, null));
            };
        }

        log.warn("Statut/structure Flutterwave inattendu pour tx_ref {} (status={}, ni data ni meta.authorization).",
                paymentReference, response.getStatus());
        return new ChargeResult(ChargeStatus.FAILED, paymentReference, payerReferenceLast4,
                "Statut ou structure gateway inattendu : " + response.getStatus(), null);
    }

    /** Étape 2 : soumet le PIN carte (ou AVS) puis vérifie la transaction. */
    ChargeResult completeWithPin(String paymentReference, String cardNumber, String cvv, String expiry,
                                 String currency, java.math.BigDecimal amount, String fullName,
                                 String email, String cardPin) {
        CardRequest cardRequest = new CardRequest(cardNumber, cvv, expiry.split("/")[0], expiry.split("/")[1],
                currency, amount, fullName, email, paymentReference, properties.redirectUrl(), null);
        cardRequest.setAuthorization(new Authorization().pinAuthorization(cardPin));

        Response response;
        try {
            response = new CardCharge().runTransaction(cardRequest);
        } catch (Exception e) {
            log.error("Erreur lors de la validation PIN carte pour tx_ref {}", paymentReference, e);
            return new ChargeResult(ChargeStatus.FAILED, paymentReference, null,
                    "Erreur de communication avec le gateway", null);
        }

        if (response == null) {
            log.error("Réponse Flutterwave nulle (validation PIN) pour tx_ref {}", paymentReference);
            return new ChargeResult(ChargeStatus.FAILED, paymentReference, null, "Réponse gateway vide", null);
        }

        log.info("Réponse Flutterwave carte (completeWithPin) pour tx_ref {} : status={}, message={}",
                paymentReference, response.getStatus(), response.getMessage());

        if (STATUS_ERROR.equalsIgnoreCase(response.getStatus())) {
            log.warn("Validation PIN rejetée par Flutterwave pour tx_ref {} : {}",
                    paymentReference, response.getMessage());
            return new ChargeResult(ChargeStatus.FAILED, paymentReference, null, response.getMessage(), null);
        }

        if (!STATUS_SUCCESS.equalsIgnoreCase(response.getStatus())) {
            log.warn("Statut Flutterwave inattendu (completeWithPin) pour tx_ref {} (status={}).",
                    paymentReference, response.getStatus());
            return new ChargeResult(ChargeStatus.FAILED, paymentReference, null,
                    "Statut gateway inattendu : " + response.getStatus(), null);
        }

        // Le PIN peut lui-même être accepté puis suivi d'une étape d'authorization supplémentaire
        // (ex: OTP) - la réponse a alors la même forme meta.authorization-only qu'un initiate(),
        // pas du tout la forme data.status attendue pour une charge réglée. resolveChargeOutcome
        // couvre les deux formes plutôt que de traiter la seconde comme un échec.
        return resolveChargeOutcome(response, paymentReference, null);
    }
}