package com.guentours.payment.gateway.stripe;

import com.guentours.payment.gateway.AuthorizationChallenge;
import com.guentours.payment.gateway.ChargeRequest;
import com.guentours.payment.gateway.ChargeResult;
import com.guentours.payment.gateway.PaymentGateway;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Set;

/**
 * Card / Google Pay / Apple Pay / PayPal via Stripe PaymentIntents. Unlike Flutterwave's model
 * (raw card PAN/CVV posted to our backend), Stripe expects the opposite: this gateway only creates
 * the PaymentIntent and hands its {@code client_secret} back (see {@link AuthorizationChallenge#clientAction}) -
 * the frontend confirms it directly with Stripe via Stripe.js, so the card number never reaches
 * this backend. That confirmation is what the {@code payment_intent.succeeded}/{@code
 * payment_intent.payment_failed} webhook (see {@code StripeWebhookController}) ultimately reports
 * back through {@code PaymentService#confirmFromGatewayCallback}.
 *
 * <p>One code path handles CARD, GOOGLE_PAY, APPLE_PAY and PAYPAL alike: with {@code
 * automatic_payment_methods} enabled, Stripe.js itself decides which of those to render (a wallet
 * button only appears when the browser/device actually supports it) - there is no separate
 * "Google Pay API call" the way Flutterwave modeled it.
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

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(toSmallestUnit(request.amount(), request.currency()))
                .setCurrency(request.currency().toLowerCase(Locale.ROOT))
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build())
                .setDescription("Paiement GuenTours - " + request.paymentMethod())
                .putMetadata("paymentId", request.paymentReference())
                .build();

        PaymentIntent intent;
        try {
            intent = PaymentIntent.create(params, requestOptions);
        } catch (StripeException e) {
            log.error("Erreur Stripe lors de la création du PaymentIntent pour le payment {}",
                    request.paymentReference(), e);
            return ChargeResult.declined("Erreur de communication avec Stripe : " + e.getMessage());
        }

        if (intent.getClientSecret() == null) {
            log.error("Stripe n'a renvoyé aucun client_secret pour le payment {} (PaymentIntent {})",
                    request.paymentReference(), intent.getId());
            return ChargeResult.declined("Réponse Stripe inattendue (client_secret manquant)");
        }

        return ChargeResult.pendingAuthorization(intent.getId(), AuthorizationChallenge.clientAction(intent.getClientSecret()));
    }

    /** Stripe wants amounts in the smallest currency unit (cents) - except zero-decimal currencies
     *  like XAF/XOF/JPY, which it wants in whole units already. */
    private long toSmallestUnit(BigDecimal amount, String currency) {
        boolean zeroDecimal = ZERO_DECIMAL_CURRENCIES.contains(currency.toUpperCase(Locale.ROOT));
        BigDecimal scaled = zeroDecimal ? amount : amount.multiply(BigDecimal.valueOf(100));
        return scaled.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
