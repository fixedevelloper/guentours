package com.guentours.payment.web;

import com.flutterwave.bean.Response;
import com.flutterwave.services.Transactions;
import com.guentours.payment.gateway.flutterwave.FlutterwaveProperties;
import com.guentours.payment.gateway.flutterwave.FlutterwaveWebhookPayload;
import com.guentours.payment.gateway.ChargeResult;
import com.guentours.payment.gateway.ChargeStatus;
import com.guentours.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/payments/webhooks/flutterwave")
@RequiredArgsConstructor
public class FlutterwaveWebhookController {

    private static final String SIGNATURE_HEADER = "verif-hash";
    private static final String STATUS_ERROR = "error";
    private static final String STATUS_SUCCESS = "success";

    private final PaymentService paymentService;
    private final FlutterwaveProperties properties;

    @PostMapping
    public ResponseEntity<Void> handle(
            @RequestHeader(value = SIGNATURE_HEADER, required = false) String signature,
            @RequestBody FlutterwaveWebhookPayload payload) {

        if (signature == null || !signature.equals(properties.webhookSecretHash())) {
            log.warn("Webhook Flutterwave refusé : signature invalide ou absente.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (payload == null || payload.data() == null || payload.data().tx_ref() == null) {
            log.warn("Webhook Flutterwave reçu sans tx_ref exploitable, ignoré. payload={}", payload);
            return ResponseEntity.ok().build();
        }

        String txRef = payload.data().tx_ref();

        // On ne fait jamais confiance au webhook seul : on revérifie via l'API avant toute action métier.
        Response verification;
        try {
            verification = new Transactions().runVerifyTransaction(Math.toIntExact(payload.data().id()));
        } catch (Exception e) {
            log.error("Échec de la vérification Flutterwave pour tx_ref {} (id={})",
                    txRef, payload.data().id(), e);
            // On renvoie 500 pour que Flutterwave retente le webhook plus tard — on ne veut pas
            // acquitter un événement qu'on n'a pas réussi à vérifier.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        if (verification == null) {
            log.error("Réponse de vérification Flutterwave nulle pour tx_ref {}", txRef);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        log.info("Vérification Flutterwave pour tx_ref {} : status={}, message={}",
                txRef, verification.getStatus(), verification.getMessage());

        if (STATUS_ERROR.equalsIgnoreCase(verification.getStatus())) {
            log.warn("Vérification Flutterwave en erreur pour tx_ref {} : {}",
                    txRef, verification.getMessage());
            paymentService.confirmFromGatewayCallback(txRef,
                    new ChargeResult(ChargeStatus.FAILED, txRef, null, verification.getMessage(), null));
            return ResponseEntity.ok().build();
        }

        if (!STATUS_SUCCESS.equalsIgnoreCase(verification.getStatus()) || verification.getData() == null) {
            log.warn("Statut/structure de vérification inattendu pour tx_ref {} (status={}), traité comme échec.",
                    txRef, verification.getStatus());
            paymentService.confirmFromGatewayCallback(txRef,
                    new ChargeResult(ChargeStatus.FAILED, txRef, null,
                            "Statut de vérification inattendu : " + verification.getStatus(), null));
            return ResponseEntity.ok().build();
        }

        // ⚠️ getStatus() sur Data supposé d'après les conventions du SDK vues sur CardCharge/MobileMoney
        String flwTransactionStatus = verification.getData().getStatus();
        ChargeStatus status = switch (flwTransactionStatus) {
            case "successful" -> ChargeStatus.SUCCEEDED;
            case "pending" -> ChargeStatus.PENDING;
            default -> ChargeStatus.FAILED;
        };

        ChargeResult result = new ChargeResult(status, txRef, null,
                status == ChargeStatus.FAILED ? "Paiement rejeté par Flutterwave (transaction status: "
                        + flwTransactionStatus + ")" : null, null);

        paymentService.confirmFromGatewayCallback(txRef, result);

        return ResponseEntity.ok().build();
    }
}