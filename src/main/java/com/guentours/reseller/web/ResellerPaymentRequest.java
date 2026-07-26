package com.guentours.reseller.web;

public record ResellerPaymentRequest(
        String paymentMethod, // "CARD" | "MTN_MOBILE_MONEY" | "ORANGE_MONEY"
        String payerReference  // numéro de téléphone ou token carte, selon paymentMethod
) {}