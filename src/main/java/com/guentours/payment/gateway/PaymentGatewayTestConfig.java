package com.guentours.payment.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Registers {@link MockPaymentGateway} under both provider bean names live routing can resolve to
 * in the test profile: "STRIPE" ({@link com.guentours.payment.service.PaymentProviderRoutingService#DEFAULT_PROVIDER})
 * and "FLUTTERWAVE" (kept for MOBILE_MONEY, which Stripe doesn't support). Separate instances -
 * the mock is stateless, so sharing one wouldn't matter, but two beans keeps each provider name
 * independently swappable later without touching the other.
 */
@Configuration
@Profile("test")
class PaymentGatewayTestConfig {

    @Bean("STRIPE")
    PaymentGateway stripeMockGateway() {
        return new MockPaymentGateway();
    }

    @Bean("FLUTTERWAVE")
    PaymentGateway flutterwaveMockGateway() {
        return new MockPaymentGateway();
    }
}
