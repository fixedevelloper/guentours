package com.guentours.payment.gateway;

public record AuthorizationChallenge(
        AuthorizationType type,
        String redirectUrl // renseigné uniquement si type == REDIRECT
) {
    public enum AuthorizationType { PIN, AVS, REDIRECT }
}