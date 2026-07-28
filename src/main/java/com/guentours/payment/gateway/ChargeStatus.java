package com.guentours.payment.gateway;

public enum ChargeStatus {
    SUCCEEDED,
    FAILED,
    PENDING,               // ex: mobile money en attente de validation USSD par le client
    PENDING_AUTHORIZATION  // carte : il manque un PIN/OTP/AVS avant de pouvoir continuer
}