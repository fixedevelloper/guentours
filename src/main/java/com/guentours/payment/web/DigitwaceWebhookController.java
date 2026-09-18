package com.guentours.payment.web;

import com.guentours.payment.domain.Payment;
import com.guentours.payment.gateway.ChargeResult;
import com.guentours.payment.gateway.digitwace.DigitwaceStatusClient;
import com.guentours.payment.gateway.digitwace.DigitwaceWebhookPayload;
import com.guentours.payment.service.PaymentService;
import com.guentours.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * WacePay PayIn (Digitwace) doesn't publish a webhook signature scheme on its (client-rendered) API
 * reference pages, so - unlike {@code StripeWebhookController}/{@code FlutterwaveWebhookController},
 * which verify a signature header first - this never trusts the webhook body's declared status by
 * itself: it re-verifies the transaction directly with WacePay PayIn ({@link DigitwaceStatusClient})
 * before applying any business effect. Crucially, {@code transactionId} and {@code externalReference}
 * (the paymentId) are both attacker-controlled in an unsigned request, so the two must never be
 * trusted as a pair on the caller's say-so alone: this only confirms a payment using the
 * {@code transactionId} that was actually recorded against it at charge-initiation time ({@link
 * Payment#getGatewayReference()}, set via {@code DigitwacePaymentGateway}) - otherwise a real but
 * unrelated WacePay transaction (e.g. one the attacker legitimately completed themselves) could be
 * replayed to confirm someone else's payment/booking. {@code externalReference} is expected to echo
 * back the {@code paymentReference} (= {@code Payment#getId()}) sent in the init-payment request -
 * confirm that field name against the real payload once WacePay's schema is available, and add
 * signature verification here too if Digitwace turns out to provide one.
 */
@Slf4j
@RestController
@RequestMapping("/api/payments/webhooks/digitwace")
@RequiredArgsConstructor
public class DigitwaceWebhookController {

    private final PaymentService paymentService;
    private final DigitwaceStatusClient statusClient;

    @PostMapping
    public ResponseEntity<Void> handle(@RequestBody DigitwaceWebhookPayload payload) {
        String paymentId = payload != null ? payload.externalReference() : null;
        String transactionId = payload != null ? payload.transactionId() : null;

        log.info("Webhook WacePay PayIn reçu : paymentId={}, transactionId={}, status déclaré={}",
                paymentId, transactionId, payload != null ? payload.status() : null);

        if (paymentId == null) {
            log.warn("Webhook WacePay PayIn reçu sans externalReference exploitable, ignoré. payload={}", payload);
            return ResponseEntity.ok().build();
        }

        Payment payment;
        try {
            payment = paymentService.getById(paymentId);
        } catch (NotFoundException e) {
            log.warn("Webhook WacePay PayIn reçu pour un paymentId inconnu {}, ignoré.", paymentId);
            return ResponseEntity.ok().build();
        }

        // Le webhook n'étant pas signé, on ne confirme jamais un paiement avec un transactionId qui
        // ne correspond pas à celui enregistré lors de l'initiation de CE paiement précis - sinon un
        // attaquant pourrait rejouer le transactionId d'une transaction WacePay distincte (bien
        // réelle, mais sans rapport) pour faire confirmer le paiement/booking de quelqu'un d'autre.
        if (transactionId == null || !transactionId.equals(payment.getGatewayReference())) {
            log.warn("Webhook WacePay PayIn refusé pour paymentId {} : transactionId {} ne correspond pas "
                            + "à la référence gateway enregistrée pour ce paiement ({}).",
                    paymentId, transactionId, payment.getGatewayReference());
            return ResponseEntity.ok().build();
        }

        ChargeResult result;
        try {
            result = statusClient.verify(transactionId);
        } catch (Exception e) {
            log.error("Échec de la vérification WacePay PayIn pour paymentId {} (transactionId={})",
                    paymentId, transactionId, e);
            // On renvoie 500 pour que Digitwace retente le webhook plus tard - on ne veut pas
            // acquitter un événement qu'on n'a pas réussi à vérifier.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        paymentService.confirmFromGatewayCallback(paymentId, result);
        log.info("Webhook WacePay PayIn traité pour paymentId {} : décision finale={}", paymentId, result.status());

        return ResponseEntity.ok().build();
    }
}
