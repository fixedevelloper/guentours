package com.guentours.payment.gateway;

public record ChargeResult(boolean success, String gatewayReference, String failureReason) {

    public static ChargeResult success(String gatewayReference) {
        return new ChargeResult(true, gatewayReference, null);
    }

    public static ChargeResult declined(String reason) {
        return new ChargeResult(false, null, reason);
    }
}
