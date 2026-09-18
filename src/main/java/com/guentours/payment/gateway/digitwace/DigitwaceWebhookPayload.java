package com.guentours.payment.gateway.digitwace;

/** Field names are a best-effort guess pending WacePay PayIn's real webhook payload schema - see {@link DigitwaceProperties}. */
public record DigitwaceWebhookPayload(
        String transactionId,
        String externalReference,
        String status
) {}
