package com.guentours.payment.gateway;

public record AuthorizationChallenge(
        AuthorizationType type,
        String redirectUrl,   // renseigné uniquement si type == REDIRECT
        String clientSecret   // renseigné uniquement si type == CLIENT_ACTION (Stripe Checkout Session)
) {
    public AuthorizationChallenge(AuthorizationType type, String redirectUrl) {
        this(type, redirectUrl, null);
    }

    public static AuthorizationChallenge clientAction(String clientSecret) {
        return new AuthorizationChallenge(AuthorizationType.CLIENT_ACTION, null, clientSecret);
    }

    /** {@code CLIENT_ACTION} is Stripe's model: the frontend must mount Stripe's Embedded Checkout
     *  with {@code clientSecret} itself rather than being redirected or prompted for a PIN. */
    public enum AuthorizationType { PIN, AVS, REDIRECT, OTP, CLIENT_ACTION }
}