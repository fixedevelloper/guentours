package com.guentours.payment.gateway.digitwace;

/** Field names are a best-effort guess pending WacePay PayIn's real response schema - see {@link DigitwaceProperties}. */
record DigitwaceInitPaymentResponse(
        String transactionId,
        String status,
        String message
) {}
