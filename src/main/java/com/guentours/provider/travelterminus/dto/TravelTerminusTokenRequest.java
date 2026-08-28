package com.guentours.provider.travelterminus.dto;

/** Body for {@code POST /api/auth/generate-token} - the only endpoint that accepts raw credentials. */
public record TravelTerminusTokenRequest(String apiKey, String secretKey) {
}
