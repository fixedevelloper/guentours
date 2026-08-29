package com.guentours.payment.web;

import com.guentours.payment.gateway.ChargeResult;
import com.guentours.payment.gateway.ChargeStatus;
import com.guentours.payment.gateway.stripe.StripeProperties;
import com.guentours.payment.service.PaymentService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * Confirms Stripe PaymentIntents asynchronously - the frontend's {@code stripe.confirmPayment}
 * call talks directly to Stripe, never to us, so this webhook is the only way this backend learns
 * the final outcome (mirrors {@link FlutterwaveWebhookController}'s role for Flutterwave).
 *
 * <p>Unlike Flutterwave's {@code verif-hash} (a static shared-secret header), Stripe signs the
 * exact raw request body (HMAC-SHA256, {@code Stripe-Signature} header) - {@code payload} must
 * stay the unparsed body string for {@link com.stripe.net.Webhook#constructEvent} to verify it;
 * binding it as a typed object here would let Spring reformat/reorder the JSON first and break
 * the signature check.
 */
@Slf4j
@RestController
@RequestMapping("/api/payments/webhooks/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {

    private static final String SIGNATURE_HEADER = "Stripe-Signature";

    private final PaymentService paymentService;
    private final StripeProperties properties;

    @PostMapping
    public ResponseEntity<Void> handle(
            @RequestHeader(value = SIGNATURE_HEADER, required = false) String signature,
            @RequestBody String payload) {

        if (signature == null) {
            log.warn("Webhook Stripe reçu sans en-tête {}, rejeté.", SIGNATURE_HEADER);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Event event;
        try {
            event = com.stripe.net.Webhook.constructEvent(payload, signature, properties.webhookSecret());
        } catch (SignatureVerificationException e) {
            log.warn("Webhook Stripe refusé : signature invalide ({}).", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Webhook Stripe reçu : type={}, id={}", event.getType(), event.getId());

        if (!"payment_intent.succeeded".equals(event.getType()) && !"payment_intent.payment_failed".equals(event.getType())) {
            // Autres événements (payment_method.attached, etc.) : rien à faire, mais on répond 200
            // pour que Stripe ne les retente pas indéfiniment.
            return ResponseEntity.ok().build();
        }

        Optional<StripeObject> dataObject = event.getDataObjectDeserializer().getObject();
        if (dataObject.isEmpty() || !(dataObject.get() instanceof PaymentIntent intent)) {
            log.warn("Webhook Stripe {} sans PaymentIntent désérialisable, ignoré (event id={}).",
                    event.getType(), event.getId());
            return ResponseEntity.ok().build();
        }

        String paymentId = intent.getMetadata() != null ? intent.getMetadata().get("paymentId") : null;
        if (paymentId == null) {
            log.warn("PaymentIntent {} sans metadata.paymentId, impossible de corréler, ignoré.", intent.getId());
            return ResponseEntity.ok().build();
        }

        ChargeStatus status = "payment_intent.succeeded".equals(event.getType())
                ? ChargeStatus.SUCCEEDED : ChargeStatus.FAILED;
        String failureReason = status == ChargeStatus.FAILED
                ? Optional.ofNullable(intent.getLastPaymentError())
                        .map(com.stripe.model.StripeError::getMessage)
                        .orElse("Paiement refusé par Stripe")
                : null;

        paymentService.confirmFromGatewayCallback(paymentId,
                new ChargeResult(status, intent.getId(), null, failureReason, null));
        log.info("Webhook Stripe traité pour payment {} (PaymentIntent {}) : décision finale={}",
                paymentId, intent.getId(), status);

        return ResponseEntity.ok().build();
    }
}
