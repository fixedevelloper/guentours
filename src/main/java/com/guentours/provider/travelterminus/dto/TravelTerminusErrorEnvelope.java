package com.guentours.provider.travelterminus.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Shape of every Travel Terminus error response (400/401/402/403/404/429/503). The docs example
 *  shows {@code "code":"invalid_credentials"}, but the real Stage sandbox returns
 *  {@code "errorCode":"BAD_REQUEST"} instead (e.g. {@code {"success":false,"statusCode":400,
 *  "errorCode":"BAD_REQUEST","message":"...","retryable":false,"errors":[{"field":"apiKey",
 *  "message":"...","code":"BAD_REQUEST"}]}}) - {@code @JsonAlias} accepts either. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TravelTerminusErrorEnvelope(
        Boolean success,
        Boolean status,
        Integer statusCode,
        @JsonAlias("errorCode") String code,
        String message,
        Boolean retryable,
        List<ErrorDetail> errors,
        String timestamp
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ErrorDetail(String message, String code) {
    }
}
