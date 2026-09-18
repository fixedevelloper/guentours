package com.guentours.payment.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Registers {@link MockPaymentGateway} under every provider bean name live routing can resolve to
 * in the test profile: "STRIPE" ({@link com.guentours.payment.service.PaymentProviderRoutingService#DEFAULT_PROVIDER}),
 * "FLUTTERWAVE" (kept for MOBILE_MONEY, which Stripe doesn't support), and "DIGITWACE" (WacePay
 * PayIn, an alternative MOBILE_MONEY route an admin can configure per-country). Separate instances -
 * the mock is stateless, so sharing one wouldn't matter, but distinct beans keep each provider name
 * independently swappable later without touching the others.
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

    @Bean("DIGITWACE")
    PaymentGateway digitwaceMockGateway() {
        return new MockPaymentGateway();
    }
}
