package com.guentours.provider;

import java.util.Map;

/**
 * IATA airline-code -> display-name lookup, for providers whose search response only carries the
 * two-letter carrier code (e.g. Travelport's {@code CatalogProductOfferings.ReferenceListFlight}) -
 * unlike TravelTerminus, which returns the airline's display name directly in its own payload.
 * Deliberately static: airline codes are a stable, publicly assigned namespace (IATA), not
 * something that needs a synced reference-data table like {@link com.guentours.geo.Airport}. An
 * unknown code resolves to {@code null}, left for the caller (or the frontend's own fallback) to
 * display the raw code instead.
 */
public final class IataAirlines {

    private static final Map<String, String> NAMES = Map.ofEntries(
            Map.entry("AA", "American Airlines"),
            Map.entry("AC", "Air Canada"),
            Map.entry("AF", "Air France"),
            Map.entry("AI", "Air India"),
            Map.entry("AM", "Aeromexico"),
            Map.entry("AR", "Aerolineas Argentinas"),
            Map.entry("AS", "Alaska Airlines"),
            Map.entry("AT", "Royal Air Maroc"),
            Map.entry("AV", "Avianca"),
            Map.entry("AY", "Finnair"),
            Map.entry("AZ", "ITA Airways"),
            Map.entry("B6", "JetBlue Airways"),
            Map.entry("BA", "British Airways"),
            Map.entry("BR", "EVA Air"),
            Map.entry("CA", "Air China"),
            Map.entry("CI", "China Airlines"),
            Map.entry("CM", "Copa Airlines"),
            Map.entry("CX", "Cathay Pacific"),
            Map.entry("CZ", "China Southern Airlines"),
            Map.entry("DL", "Delta Air Lines"),
            Map.entry("EK", "Emirates"),
            Map.entry("ET", "Ethiopian Airlines"),
            Map.entry("EY", "Etihad Airways"),
            Map.entry("FR", "Ryanair"),
            Map.entry("GA", "Garuda Indonesia"),
            Map.entry("GF", "Gulf Air"),
            Map.entry("HA", "Hawaiian Airlines"),
            Map.entry("IB", "Iberia"),
            Map.entry("JL", "Japan Airlines"),
            Map.entry("JJ", "LATAM Airlines Brasil"),
            Map.entry("KE", "Korean Air"),
            Map.entry("KL", "KLM Royal Dutch Airlines"),
            Map.entry("KQ", "Kenya Airways"),
            Map.entry("KU", "Kuwait Airways"),
            Map.entry("LA", "LATAM Airlines"),
            Map.entry("LH", "Lufthansa"),
            Map.entry("LO", "LOT Polish Airlines"),
            Map.entry("LX", "Swiss International Air Lines"),
            Map.entry("LY", "El Al"),
            Map.entry("MS", "EgyptAir"),
            Map.entry("MU", "China Eastern Airlines"),
            Map.entry("NH", "All Nippon Airways"),
            Map.entry("NZ", "Air New Zealand"),
            Map.entry("OK", "Czech Airlines"),
            Map.entry("OS", "Austrian Airlines"),
            Map.entry("OZ", "Asiana Airlines"),
            Map.entry("PC", "Pegasus Airlines"),
            Map.entry("PR", "Philippine Airlines"),
            Map.entry("QF", "Qantas"),
            Map.entry("QR", "Qatar Airways"),
            Map.entry("RJ", "Royal Jordanian"),
            Map.entry("RO", "TAROM"),
            Map.entry("SA", "South African Airways"),
            Map.entry("SK", "SAS Scandinavian Airlines"),
            Map.entry("SN", "Brussels Airlines"),
            Map.entry("SQ", "Singapore Airlines"),
            Map.entry("SU", "Aeroflot"),
            Map.entry("SV", "Saudia"),
            Map.entry("TG", "Thai Airways"),
            Map.entry("TK", "Turkish Airlines"),
            Map.entry("TP", "TAP Air Portugal"),
            Map.entry("UA", "United Airlines"),
            Map.entry("UL", "SriLankan Airlines"),
            Map.entry("UX", "Air Europa"),
            Map.entry("VA", "Virgin Australia"),
            Map.entry("VN", "Vietnam Airlines"),
            Map.entry("VS", "Virgin Atlantic"),
            Map.entry("WN", "Southwest Airlines"),
            Map.entry("WY", "Oman Air"),
            Map.entry("W6", "Wizz Air"),
            Map.entry("U2", "easyJet"),
            Map.entry("VY", "Vueling")
    );

    private IataAirlines() {
    }

    /** Returns the airline's display name for {@code carrierCode}, or {@code null} if unknown. */
    public static String nameFor(String carrierCode) {
        return carrierCode == null ? null : NAMES.get(carrierCode.toUpperCase());
    }
}
