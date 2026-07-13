package com.guentours.provider.travelopro;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Request body for Travelopro's {@code /api/aeroVE5/book} endpoint - creates the
 * reservation against the fare quoted at search time. Field names follow the same
 * convention as the confirmed Availability contract (snake_case auth fields, PascalCase
 * business fields); unlike Availability this shape has not been validated against a
 * live sandbox response yet, so double-check it against Travelopro's Book API docs
 * before going live.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record TraveloproBookRequest(
        String user_id,
        String user_password,
        String access,
        String ip_address,
        String FareSourceCode,
        String ContactEmail,
        List<Passenger> Passengers
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Passenger(
            String Title,
            String FirstName,
            String LastName,
            String PaxType,
            String DateOfBirth,
            String PassportNo
    ) {
    }
}
