package com.guentours.provider.travelterminus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Shape of every Travel Terminus error response (400/401/402/403/404/429/503), e.g.
 *  {@code {"status":false,"statusCode":401,"code":"invalid_credentials","message":"...",
 *  "retryable":false,"errors":[{"message":"Unauthorized","code":"UNAUTHORIZED"}]}}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TravelTerminusErrorEnvelope(
        Boolean success,
        Boolean status,
        Integer statusCode,
        String code,
        String message,
        Boolean retryable,
        List<ErrorDetail> errors,
        String timestamp
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ErrorDetail(String message, String code) {
    }
}
