package com.guentours.payment.gateway.stripe;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.stripe")
public record StripeProperties(
        String secretKey,
        String publishableKey,
        String webhookSecret,
        String redirectUrl
) {
}
