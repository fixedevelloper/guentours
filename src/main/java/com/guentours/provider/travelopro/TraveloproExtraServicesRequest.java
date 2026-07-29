package com.guentours.provider.travelopro;

/** Body for {@code POST /api/aeroVE5/extra_services}, used here to fetch the real seat map. */
public record TraveloproExtraServicesRequest(String session_id, String fare_source_code) {}
