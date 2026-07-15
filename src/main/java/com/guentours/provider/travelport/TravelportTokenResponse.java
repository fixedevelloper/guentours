package com.guentours.provider.travelport;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Response body of Travelport's OAuth2 token endpoint {@code POST /oauth/oauth20/token}. */
@JsonIgnoreProperties(ignoreUnknown = true)
record TravelportTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresInSeconds
) {
}
