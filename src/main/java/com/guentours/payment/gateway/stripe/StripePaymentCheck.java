package com.guentours.payment.gateway.stripe;

import com.guentours.payment.domain.Payment;
import com.guentours.payment.domain.PaymentAuthorizationType;
import com.guentours.payment.domain.PaymentRepository;
import com.guentours.payment.domain.PaymentStatus;
import com.guentours.payment.gateway.ChargeResult;
import com.guentours.payment.gateway.ChargeStatus;
import com.guentours.payment.service.PaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeError;
import com.stripe.net.RequestOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Actively polls Stripe for payments stuck in {@code PENDING_AUTHORIZATION}/{@code CLIENT_ACTION}
 * (see {@link StripePaymentGateway}), the same safety-net role {@code PaymentCheck} already plays
 * for Flutterwave. Necessary because {@code stripe.confirmPayment} on the frontend talks directly
 * to Stripe - our backend only learns the outcome via the {@code payment_intent.succeeded}/{@code
 * .payment_failed} webhook (see {@link com.guentours.payment.web.StripeWebhookController}), which
 * Stripe can only reach on a publicly-addressable HTTPS URL. On localhost (no {@code stripe
 * listen} tunnel configured), that webhook never fires and the payment - and the booking summary
 * page waiting on it - would otherwise stay stuck forever with no other way to resolve.
 */
@Slf4j
@Component
public class StripePaymentCheck {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final StripeProperties properties;

    public StripePaymentCheck(PaymentRepository paymentRepository, PaymentService paymentService,
                              StripeProperties properties) {
        this.paymentRepository = paymentRepository;
        this.paymentService = paymentService;
        this.properties = properties;
    }

    @Scheduled(fixedRate = 20_000, initialDelay = 20_000)
    public void checkPendingStripePayments() {
        if (properties.secretKey() == null || properties.secretKey().isBlank()) {
            return;
        }
        List<Payment> pending = paymentRepository.findByStatus(PaymentStatus.PENDING_AUTHORIZATION);
        for (Payment payment : pending) {
            if (payment.getAuthorizationType() == PaymentAuthorizationType.CLIENT_ACTION) {
                checkOne(payment);
            }
        }
    }

    private void checkOne(Payment payment) {
        if (payment.getGatewayReference() == null) {
            return;
        }
        RequestOptions options = RequestOptions.builder().setApiKey(properties.secretKey()).build();
        PaymentIntent intent;
        try {
            intent = PaymentIntent.retrieve(payment.getGatewayReference(), options);
        } catch (StripeException e) {
            log.warn("Échec de vérification Stripe pour le paiement {} (PaymentIntent {})",
                    payment.getId(), payment.getGatewayReference(), e);
            return;
        }

        ChargeStatus status = switch (intent.getStatus()) {
            case "succeeded" -> ChargeStatus.SUCCEEDED;
            case "canceled" -> ChargeStatus.FAILED;
            // requires_payment_method/requires_confirmation/requires_action/processing: the payer
            // hasn't finished (or Stripe is still finalizing) - leave PENDING_AUTHORIZATION as-is.
            default -> null;
        };
        if (status == null) {
            return;
        }

        String failureReason = status == ChargeStatus.FAILED
                ? Optional.ofNullable(intent.getLastPaymentError()).map(StripeError::getMessage)
                        .orElse("Paiement annulé")
                : null;

        paymentService.confirmFromGatewayCallback(payment.getId(),
                new ChargeResult(status, intent.getId(), null, failureReason, null));
        log.info("Payment {} résolu par polling Stripe (PaymentIntent {}) : {}",
                payment.getId(), intent.getId(), status);
    }
}
