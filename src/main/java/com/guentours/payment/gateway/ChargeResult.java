package com.guentours.payment.gateway;

public record ChargeResult(
        ChargeStatus status,
        String gatewayReference,      // payment.getId() / tx_ref
        String payerReferenceLast4,
        String failureReason,
        AuthorizationChallenge authorizationChallenge // Présent uniquement si PENDING_AUTHORIZATION
) {
    public boolean isSucceeded() { return status == ChargeStatus.SUCCEEDED; }
    public boolean isFailed() { return status == ChargeStatus.FAILED; }
    public boolean isPending() { return status == ChargeStatus.PENDING; }
    public boolean requiresAuthorization() { return status == ChargeStatus.PENDING_AUTHORIZATION; }

    // Fabriques statiques utilitaires
    public static ChargeResult success(String gatewayReference) {
        return new ChargeResult(ChargeStatus.SUCCEEDED, gatewayReference, null, null, null);
    }

    public static ChargeResult success(String gatewayReference, String last4) {
        return new ChargeResult(ChargeStatus.SUCCEEDED, gatewayReference, last4, null, null);
    }

    public static ChargeResult declined(String failureReason) {
        return new ChargeResult(ChargeStatus.FAILED, null, null, failureReason, null);
    }

    public static ChargeResult pendingAuthorization(String gatewayReference, AuthorizationChallenge challenge) {
        return new ChargeResult(ChargeStatus.PENDING_AUTHORIZATION, gatewayReference, null, null, challenge);
    }
}