package com.guentours.payment.gateway.stripe;

import com.guentours.payment.domain.Payment;
import com.guentours.payment.gateway.AuthorizationChallenge;
import com.guentours.payment.gateway.ChargeRequest;
import com.guentours.payment.gateway.ChargeResult;
import com.guentours.payment.gateway.PaymentGateway;
import com.guentours.shared.exception.BusinessException;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/**
 * Card / Google Pay / Apple Pay / PayPal via a Stripe Checkout Session rendered in {@code
 * embedded_page} ui_mode. Unlike Flutterwave's model (raw card PAN/CVV posted to our backend),
 * Stripe expects the opposite: this gateway only creates the Session and hands its {@code
 * client_secret} back (see {@link AuthorizationChallenge#clientAction}) - the frontend mounts
 * Stripe's own Embedded Checkout with it (see {@code StripeCheckoutDialog.tsx}), which renders
 * Stripe's full payment UI (card/wallet fields, PayPal, error states) inline in our own page
 * without a full navigation away, so card/wallet details never reach this backend. That
 * confirmation is what the {@code checkout.session.completed}/{@code .async_payment_succeeded}/
 * {@code .async_payment_failed} webhook (see {@code StripeWebhookController}) ultimately reports
 * back through {@code PaymentService#confirmFromGatewayCallback}. {@code returnUrl} below is only
 * actually used by the small set of payment methods that still need a full-page bounce (e.g.
 * certain bank redirects) - it points at the same provider-agnostic landing page Flutterwave's 3DS
 * redirect already uses (see {@code FlutterwaveProperties#redirectUrl}).
 *
 * <p>One code path handles CARD, GOOGLE_PAY, APPLE_PAY and PAYPAL alike: Stripe's Embedded Checkout
 * decides which of those to render (a wallet button only appears when the browser/device actually
 * supports it) - there is no separate "Google Pay API call" the way Flutterwave modeled it.
 * Adaptive Pricing is enabled so international payers see the amount converted to their local
 * currency automatically, without this backend having to resolve exchange rates itself.
 */
@Slf4j
@Component("STRIPE")
@Profile("!test")
public class StripePaymentGateway implements PaymentGateway {

    /** Currencies Stripe expects in whole units (no cents) rather than the smallest sub-unit -
     *  https://docs.stripe.com/currencies#zero-decimal. XAF/XOF matter here since the app serves
     *  Central/West African markets. */
    private static final Set<String> ZERO_DECIMAL_CURRENCIES = Set.of(
            "BIF", "CLP", "DJF", "GNF", "JPY", "KMF", "KRW", "MGA", "PYG", "RWF",
            "UGX", "VND", "VUV", "XAF", "XOF", "XPF");

    private final StripeProperties properties;

    public StripePaymentGateway(StripeProperties properties) {
        this.properties = properties;
    }

    @Override
    public ChargeResult charge(ChargeRequest request) {
        RequestOptions requestOptions = RequestOptions.builder().setApiKey(properties.secretKey()).build();

        // Only used by the payment methods that still require a full-page bounce - same
        // provider-agnostic landing page as the Flutterwave 3DS redirect, which just looks the
        // payment up by tx_ref and forwards to the live booking status regardless of outcome.
        String returnUrl = properties.redirectUrl() + "?tx_ref="
                + URLEncoder.encode(request.paymentReference(), StandardCharsets.UTF_8);

        SessionCreateParams params = SessionCreateParams.builder()
                .setUiMode(SessionCreateParams.UiMode.EMBEDDED_PAGE)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setReturnUrl(returnUrl)
                // The dialog detects completion itself via the Embedded Checkout onComplete
                // callback - no need to also force a full navigation away once the payer is done.
                .setRedirectOnCompletion(SessionCreateParams.RedirectOnCompletion.IF_REQUIRED)
                .setClientReferenceId(request.paymentReference())
                .putMetadata("paymentId", request.paymentReference())
                .setCustomerEmail(request.customerEmail())
                .setAdaptivePricing(SessionCreateParams.AdaptivePricing.builder().setEnabled(true).build())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(request.currency().toLowerCase(Locale.ROOT))
                                .setUnitAmount(toSmallestUnit(request.amount(), request.currency()))
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Paiement GuenTours - " + request.paymentMethod())
                                        .build())
                                .build())
                        .build())
                .build();

        Session session;
        try {
            session = Session.create(params, requestOptions);
        } catch (StripeException e) {
            log.error("Erreur Stripe lors de la création de la Checkout Session pour le payment {}",
                    request.paymentReference(), e);
            return ChargeResult.declined("Erreur de communication avec Stripe : " + e.getMessage());
        }

        if (session.getClientSecret() == null) {
            log.error("Stripe n'a renvoyé aucun client_secret pour le payment {} (Session {})",
                    request.paymentReference(), session.getId());
            return ChargeResult.declined("Réponse Stripe inattendue (client_secret manquant)");
        }

        return ChargeResult.pendingAuthorization(session.getId(),
                AuthorizationChallenge.clientAction(session.getClientSecret()));
    }

    /**
     * {@code payment.getGatewayReference()} is the Checkout Session id ({@code cs_...}), not a
     * charge/PaymentIntent id - Stripe's Refund API needs the latter, so this looks the Session back
     * up first to read the PaymentIntent it settled as (embedded_page mode always creates one for a
     * {@code payment}-mode Session once it completes).
     */
    @Override
    public void refund(Payment payment) {
        RequestOptions requestOptions = RequestOptions.builder().setApiKey(properties.secretKey()).build();
        try {
            Session session = Session.retrieve(payment.getGatewayReference(), requestOptions);
            String paymentIntentId = session.getPaymentIntent();
            if (paymentIntentId == null) {
                throw new BusinessException("Session Stripe " + session.getId()
                        + " n'a pas de PaymentIntent associé - remboursement manuel requis via le dashboard Stripe.");
            }
            Refund.create(RefundCreateParams.builder().setPaymentIntent(paymentIntentId).build(), requestOptions);
        } catch (StripeException e) {
            log.error("Erreur Stripe lors du remboursement du payment {} (Session {})",
                    payment.getId(), payment.getGatewayReference(), e);
            throw new BusinessException("Erreur de communication avec Stripe lors du remboursement : " + e.getMessage());
        }
    }

    /** Stripe wants amounts in the smallest currency unit (cents) - except zero-decimal currencies
     *  like XAF/XOF/JPY, which it wants in whole units already. */
    private long toSmallestUnit(BigDecimal amount, String currency) {
        boolean zeroDecimal = ZERO_DECIMAL_CURRENCIES.contains(currency.toUpperCase(Locale.ROOT));
        BigDecimal scaled = zeroDecimal ? amount : amount.multiply(BigDecimal.valueOf(100));
        return scaled.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
