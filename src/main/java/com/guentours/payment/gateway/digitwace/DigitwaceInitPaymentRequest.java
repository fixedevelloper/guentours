package com.guentours.payment.gateway.digitwace;

import java.math.BigDecimal;

/** Field names are a best-effort guess pending WacePay PayIn's real request schema - see {@link DigitwaceProperties}. */
record DigitwaceInitPaymentRequest(
        String externalReference,
        BigDecimal amount,
        String currency,
        String countryCode,
        String phoneNumber,
        String customerEmail,
        String description
) {}
