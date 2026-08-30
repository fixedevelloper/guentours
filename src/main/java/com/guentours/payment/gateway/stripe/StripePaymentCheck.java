package com.guentours.payment.gateway.stripe;

import com.guentours.payment.domain.Payment;
import com.guentours.payment.domain.PaymentAuthorizationType;
import com.guentours.payment.domain.PaymentRepository;
import com.guentours.payment.domain.PaymentStatus;
import com.guentours.payment.gateway.ChargeResult;
import com.guentours.payment.gateway.ChargeStatus;
import com.guentours.payment.service.PaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Actively polls Stripe for payments stuck in {@code PENDING_AUTHORIZATION}/{@code CLIENT_ACTION}
 * (see {@link StripePaymentGateway}), the same safety-net role {@code PaymentCheck} already plays
 * for Flutterwave. Necessary because the payer completes the charge in the Embedded Checkout
 * mounted client-side - our backend only learns the outcome via the {@code
 * checkout.session.completed}/{@code .async_payment_succeeded}/{@code .async_payment_failed}
 * webhook (see {@link com.guentours.payment.web.StripeWebhookController}), which Stripe can only
 * reach on a publicly-addressable HTTPS URL. On localhost (no {@code stripe listen} tunnel
 * configured), that webhook never fires and the payment - and the booking summary page waiting on
 * it - would otherwise stay stuck forever with no other way to resolve.
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

    /** Stripe Checkout Session ids always start with this prefix. A payment can still carry a
     *  {@code pi_...} PaymentIntent id here if it was created by an older build of this gateway
     *  (before the PaymentIntent -> Checkout Session migration) and never resolved before the
     *  switch - such a row is orphaned for good: the webhook no longer subscribes to {@code
     *  payment_intent.*} events, and {@code Session.retrieve} on a PaymentIntent id always 400s
     *  ("No such checkout.session: pi_..."), so retrying it every 20s forever would just spam
     *  Stripe and our logs for nothing. Fail it once instead, pointing at the raw gateway
     *  reference so it can still be reconciled by hand in the Stripe dashboard if needed. */
    private static final String STRIPE_SESSION_ID_PREFIX = "cs_";

    @Scheduled(fixedRate = 20_000, initialDelay = 20_000)
    public void checkPendingStripePayments() {
        if (properties.secretKey() == null || properties.secretKey().isBlank()) {
            return;
        }
        List<Payment> pending = paymentRepository.findByStatus(PaymentStatus.PENDING_AUTHORIZATION);
        for (Payment payment : pending) {
            if (payment.getAuthorizationType() != PaymentAuthorizationType.CLIENT_ACTION) {
                continue;
            }
            if (payment.getGatewayReference() == null || !payment.getGatewayReference().startsWith(STRIPE_SESSION_ID_PREFIX)) {
                log.warn("Payment {} en CLIENT_ACTION avec une référence gateway pré-migration ({}), "
                                + "impossible à résoudre automatiquement - marqué en échec, à réconcilier manuellement si besoin.",
                        payment.getId(), payment.getGatewayReference());
                paymentService.confirmFromGatewayCallback(payment.getId(), new ChargeResult(ChargeStatus.FAILED,
                        payment.getGatewayReference(), null, "Session de paiement introuvable (migration Stripe) - contactez le support", null));
                continue;
            }
            checkOne(payment);
        }
    }

    private void checkOne(Payment payment) {
        RequestOptions options = RequestOptions.builder().setApiKey(properties.secretKey()).build();
        Session session;
        try {
            session = Session.retrieve(payment.getGatewayReference(), options);
        } catch (StripeException e) {
            log.warn("Échec de vérification Stripe pour le paiement {} (Session {})",
                    payment.getId(), payment.getGatewayReference(), e);
            return;
        }

        ChargeStatus status;
        if ("complete".equals(session.getStatus()) && "paid".equals(session.getPaymentStatus())) {
            status = ChargeStatus.SUCCEEDED;
        } else if ("expired".equals(session.getStatus())) {
            status = ChargeStatus.FAILED;
        } else {
            // "open" (payer hasn't finished) or "complete" with an async method still settling -
            // leave PENDING_AUTHORIZATION as-is, the webhook or a later poll will resolve it.
            return;
        }

        String failureReason = status == ChargeStatus.FAILED ? "Session de paiement expirée" : null;

        paymentService.confirmFromGatewayCallback(payment.getId(),
                new ChargeResult(status, session.getId(), null, failureReason, null));
        log.info("Payment {} résolu par polling Stripe (Session {}) : {}",
                payment.getId(), session.getId(), status);
    }
}
