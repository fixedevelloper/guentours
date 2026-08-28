package com.guentours.provider.travelterminus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** {@code data} payload of the Generate Token response - {@code accessToken} is a JWT (HS256)
 *  valid for {@code expiresIn} seconds (fixed at 86400 / 24h), sent back as the {@code access-token}
 *  header on every subsequent call. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TravelTerminusTokenData(String accessToken, Long expiresIn) {
}
