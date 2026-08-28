package com.guentours.provider.travelterminus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Common {@code {"message": "...", "data": {...}}} wrapper Travel Terminus uses on every
 *  endpoint except Streaming Search (which emits raw SSE/NDJSON lines instead). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TravelTerminusEnvelope<T>(String message, T data) {
}
