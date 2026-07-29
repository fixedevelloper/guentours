package com.guentours.provider.travelopro;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Response from Travelopro's {@code /api/aeroVE5/booking} endpoint. Confirmed against the vendor's
 * own sample success/failure payloads plus a real sandbox validation failure: on success/business
 * failure the wrapper is {@code BookFlightResponse.BookFlightResult} (booking reference in
 * {@code UniqueID}, real ticketing deadline in {@code TktTimeLimit}), but a request-validation
 * failure (e.g. a missing required field) comes back as a completely different, top-level
 * {@code {"Errors":{"ErrorCode":...,"ErrorMessage":...}}} envelope with no {@code BookFlightResponse}
 * key at all - hence {@code Errors} is modeled here too, as a sibling of {@code BookFlightResponse}.
 * {@code BookFlightResult.Errors} is read as a raw {@link JsonNode} rather than a typed list because
 * the vendor serializes it as {@code ""} on success but as an array of {@code {Errors:{ErrorCode,
 * ErrorMessage}}} wrappers on a business-level booking failure - a strict list type would fail to
 * deserialize the success case. {@code Success} isn't modeled at all for the same reason (JSON
 * boolean {@code true} on success, the string {@code "false"} on failure): {@code Status}
 * ("CONFIRMED" vs {@code ""}) already tells success/failure unambiguously without that conflict.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record TraveloproBookResponse(BookFlightResponse BookFlightResponse, ValidationError Errors) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BookFlightResponse(BookFlightResult BookFlightResult) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BookFlightResult(String Status, String UniqueID, String TktTimeLimit, JsonNode Errors) {

        /**
         * First vendor error code/message, or {@code null} when Errors is empty/absent (success).
         * Handles the array-of-wrappers shape seen for a nationality-style rejection, and also a
         * plain non-blank string (some GDS reference implementations report Errors can come back
         * that way for certain failure classes) rather than the structured object.
         */
        String firstErrorMessage() {
            if (Errors == null) {
                return null;
            }
            if (Errors.isTextual()) {
                String text = Errors.asText();
                return text.isBlank() ? null : text;
            }
            if (!Errors.isArray() || Errors.isEmpty()) {
                return null;
            }
            JsonNode inner = Errors.get(0).path("Errors");
            String code = inner.path("ErrorCode").asText(null);
            String message = inner.path("ErrorMessage").asText(null);
            if (message == null) {
                return null;
            }
            return code != null ? code + ": " + message : message;
        }
    }

    /** Top-level pre-booking request-validation error (e.g. a missing required field). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ValidationError(String ErrorCode, String ErrorMessage) {

        String describe() {
            if (ErrorMessage == null) {
                return null;
            }
            return ErrorCode != null ? ErrorCode + ": " + ErrorMessage : ErrorMessage;
        }
    }
}
