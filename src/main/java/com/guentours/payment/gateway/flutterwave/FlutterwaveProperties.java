package com.guentours.payment.gateway.flutterwave;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.flutterwave")
public record FlutterwaveProperties(
        String secretKey,
        String publicKey,
        String encryptionKey,
        String webhookSecretHash,
        String defaultCurrency,
        String defaultCountry,
        String redirectUrl
) {}