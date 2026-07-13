package com.guentours.geo;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default {@link AirportDataSource}: a curated list of ~60 major world airports (real IATA
 * codes/names/cities/countries). This exists so airport search works out of the box with no
 * external network dependency; swap in a real feed by adding another implementation and
 * marking it {@code @Primary} once one is confirmed (see {@link AirportDataSource}).
 */
@Component
class SeedAirportDataSource implements AirportDataSource {

    private static final List<AirportRecord> AIRPORTS = List.of(
            new AirportRecord("CDG", "Charles de Gaulle", "Paris", "France"),
            new AirportRecord("ORY", "Orly", "Paris", "France"),
            new AirportRecord("JFK", "John F. Kennedy International", "New York", "United States"),
            new AirportRecord("EWR", "Newark Liberty International", "Newark", "United States"),
            new AirportRecord("LAX", "Los Angeles International", "Los Angeles", "United States"),
            new AirportRecord("ORD", "O'Hare International", "Chicago", "United States"),
            new AirportRecord("ATL", "Hartsfield-Jackson", "Atlanta", "United States"),
            new AirportRecord("SFO", "San Francisco International", "San Francisco", "United States"),
            new AirportRecord("MIA", "Miami International", "Miami", "United States"),
            new AirportRecord("BOS", "Logan International", "Boston", "United States"),
            new AirportRecord("SEA", "Seattle-Tacoma International", "Seattle", "United States"),
            new AirportRecord("DEN", "Denver International", "Denver", "United States"),
            new AirportRecord("LAS", "Harry Reid International", "Las Vegas", "United States"),
            new AirportRecord("IAD", "Washington Dulles International", "Washington", "United States"),
            new AirportRecord("LHR", "Heathrow", "London", "United Kingdom"),
            new AirportRecord("LGW", "Gatwick", "London", "United Kingdom"),
            new AirportRecord("FRA", "Frankfurt am Main", "Frankfurt", "Germany"),
            new AirportRecord("MUC", "Munich", "Munich", "Germany"),
            new AirportRecord("DUS", "Düsseldorf", "Dusseldorf", "Germany"),
            new AirportRecord("AMS", "Amsterdam Schiphol", "Amsterdam", "Netherlands"),
            new AirportRecord("MAD", "Adolfo Suárez Madrid-Barajas", "Madrid", "Spain"),
            new AirportRecord("BCN", "Josep Tarradellas Barcelona-El Prat", "Barcelona", "Spain"),
            new AirportRecord("FCO", "Leonardo da Vinci-Fiumicino", "Rome", "Italy"),
            new AirportRecord("MXP", "Milan Malpensa", "Milan", "Italy"),
            new AirportRecord("VCE", "Venice Marco Polo", "Venice", "Italy"),
            new AirportRecord("IST", "Istanbul", "Istanbul", "Turkey"),
            new AirportRecord("DXB", "Dubai International", "Dubai", "United Arab Emirates"),
            new AirportRecord("DOH", "Hamad International", "Doha", "Qatar"),
            new AirportRecord("SIN", "Singapore Changi", "Singapore", "Singapore"),
            new AirportRecord("HND", "Haneda", "Tokyo", "Japan"),
            new AirportRecord("NRT", "Narita International", "Tokyo", "Japan"),
            new AirportRecord("ICN", "Incheon International", "Seoul", "South Korea"),
            new AirportRecord("PEK", "Beijing Capital International", "Beijing", "China"),
            new AirportRecord("PVG", "Shanghai Pudong International", "Shanghai", "China"),
            new AirportRecord("HKG", "Hong Kong International", "Hong Kong", "Hong Kong"),
            new AirportRecord("BKK", "Suvarnabhumi", "Bangkok", "Thailand"),
            new AirportRecord("KUL", "Kuala Lumpur International", "Kuala Lumpur", "Malaysia"),
            new AirportRecord("MNL", "Ninoy Aquino International", "Manila", "Philippines"),
            new AirportRecord("DEL", "Indira Gandhi International", "Delhi", "India"),
            new AirportRecord("BOM", "Chhatrapati Shivaji Maharaj International", "Mumbai", "India"),
            new AirportRecord("SYD", "Kingsford Smith", "Sydney", "Australia"),
            new AirportRecord("MEL", "Melbourne", "Melbourne", "Australia"),
            new AirportRecord("AKL", "Auckland", "Auckland", "New Zealand"),
            new AirportRecord("GRU", "Guarulhos International", "Sao Paulo", "Brazil"),
            new AirportRecord("GIG", "Rio de Janeiro-Galeão", "Rio de Janeiro", "Brazil"),
            new AirportRecord("MEX", "Mexico City International", "Mexico City", "Mexico"),
            new AirportRecord("YYZ", "Toronto Pearson International", "Toronto", "Canada"),
            new AirportRecord("YUL", "Montréal-Trudeau International", "Montreal", "Canada"),
            new AirportRecord("ZRH", "Zurich", "Zurich", "Switzerland"),
            new AirportRecord("GVA", "Geneva", "Geneva", "Switzerland"),
            new AirportRecord("VIE", "Vienna International", "Vienna", "Austria"),
            new AirportRecord("CPH", "Copenhagen", "Copenhagen", "Denmark"),
            new AirportRecord("OSL", "Oslo Gardermoen", "Oslo", "Norway"),
            new AirportRecord("ARN", "Stockholm Arlanda", "Stockholm", "Sweden"),
            new AirportRecord("HEL", "Helsinki-Vantaa", "Helsinki", "Finland"),
            new AirportRecord("WAW", "Warsaw Chopin", "Warsaw", "Poland"),
            new AirportRecord("PRG", "Václav Havel Prague", "Prague", "Czech Republic"),
            new AirportRecord("BUD", "Budapest Ferenc Liszt International", "Budapest", "Hungary"),
            new AirportRecord("ATH", "Athens International", "Athens", "Greece"),
            new AirportRecord("LIS", "Humberto Delgado", "Lisbon", "Portugal"),
            new AirportRecord("DUB", "Dublin", "Dublin", "Ireland"),
            new AirportRecord("BRU", "Brussels", "Brussels", "Belgium"),
            new AirportRecord("JNB", "OR Tambo International", "Johannesburg", "South Africa"),
            new AirportRecord("CAI", "Cairo International", "Cairo", "Egypt"),
            new AirportRecord("CMN", "Mohammed V International", "Casablanca", "Morocco"),
            new AirportRecord("NBO", "Jomo Kenyatta International", "Nairobi", "Kenya")
    );

    @Override
    public List<AirportRecord> fetchAll() {
        return AIRPORTS;
    }
}
