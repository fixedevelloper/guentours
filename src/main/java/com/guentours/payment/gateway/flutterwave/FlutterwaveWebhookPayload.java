package com.guentours.payment.gateway.flutterwave;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FlutterwaveWebhookPayload(String event, Data data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            Long id,
            String tx_ref,     // = notre payment.getId(), c'est notre clé de corrélation
            String flw_ref,
            String status,      // "successful" | "failed"
            String currency,
            java.math.BigDecimal amount
    ) {}
}