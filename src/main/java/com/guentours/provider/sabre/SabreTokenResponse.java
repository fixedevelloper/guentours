package com.guentours.provider.sabre;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Response body of Sabre's OAuth2 client-credentials token endpoint {@code POST /v2/auth/token}. */
record SabreTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresInSeconds
) {
}
