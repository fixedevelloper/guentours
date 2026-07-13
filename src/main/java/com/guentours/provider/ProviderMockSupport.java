package com.guentours.provider;

import com.guentours.provider.dto.FlightPriceVerification;
import com.guentours.provider.dto.HotelPriceVerification;
import com.guentours.shared.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Deterministic offer generation used by every provider adapter while running in
 * "mock mode" (default until real sandbox/production credentials for Travelopro,
 * Sabre or Travelport are configured). Flight/hotel identity (airline+number,
 * hotel name) is seeded WITHOUT the provider in the key so that providers sharing
 * an airline/hotel legitimately quote the *same* product - only the price differs
 * per provider - which is exactly the case the harmonization step needs to merge.
 */
public final class ProviderMockSupport {

    private ProviderMockSupport() {
    }

    public static List<FlightOffer> flights(ProviderType type, FlightSearchCriteria criteria,
                                      List<String> airlines, double priceMultiplier) {
        List<FlightOffer> offers = new ArrayList<>();
        String cabin = criteria.cabinClass() == null ? "ECONOMY" : criteria.cabinClass();
        String currency = criteria.currency() == null ? "EUR" : criteria.currency();
        for (String airline : airlines) {
            Random r = seeded(airline, criteria.origin(), criteria.destination(), criteria.departureDate().toString());
            int flightNumber = 100 + r.nextInt(800);
            int departureHour = 5 + r.nextInt(16);
            int departureMinute = r.nextInt(4) * 15;
            int durationHours = 2 + r.nextInt(9);
            BigDecimal basePrice = BigDecimal.valueOf(120 + r.nextInt(480));
            int seats = 1 + r.nextInt(9);

            LocalDateTime departure = criteria.departureDate().atTime(departureHour, departureMinute);
            LocalDateTime arrival = departure.plusHours(durationHours).plusMinutes(r.nextInt(60));
            BigDecimal price = basePrice.multiply(BigDecimal.valueOf(priceMultiplier)).setScale(2, RoundingMode.HALF_UP);

            offers.add(new FlightOffer(
                    type,
                    "%s-%s%d-%s".formatted(type, airline, flightNumber, criteria.departureDate()),
                    airline,
                    airline + flightNumber,
                    criteria.origin(),
                    criteria.destination(),
                    departure,
                    arrival,
                    cabin,
                    new Money(price, currency),
                    seats
            ));
        }
        return offers;
    }

    public static List<HotelOffer> hotels(ProviderType type, HotelSearchCriteria criteria,
                                    List<String> hotelNames, double priceMultiplier) {
        List<HotelOffer> offers = new ArrayList<>();
        long nights = Math.max(1, criteria.checkOut().toEpochDay() - criteria.checkIn().toEpochDay());
        for (String hotelName : hotelNames) {
            Random r = seeded(hotelName, criteria.cityCode(), criteria.checkIn().toString(), criteria.checkOut().toString());
            BigDecimal nightlyRate = BigDecimal.valueOf(60 + r.nextInt(240));
            double rating = 3.0 + r.nextInt(21) / 10.0;
            BigDecimal total = nightlyRate.multiply(BigDecimal.valueOf(nights))
                    .multiply(BigDecimal.valueOf(priceMultiplier)).setScale(2, RoundingMode.HALF_UP);

            offers.add(new HotelOffer(
                    type,
                    "%s-%s-%s".formatted(type, hotelName.replace(" ", ""), criteria.checkIn()),
                    hotelName,
                    criteria.cityCode(),
                    r.nextBoolean() ? "DOUBLE" : "TWIN",
                    criteria.checkIn(),
                    criteria.checkOut(),
                    new Money(total, "EUR"),
                    Math.min(5.0, rating)
            ));
        }
        return offers;
    }

    public static FlightPriceVerification verifyFlightPrice(String flightOfferId) {
        return new FlightPriceVerification(flightOfferId, null, true, 9, "1 checked bag included");
    }

    public static HotelPriceVerification verifyHotelPrice(String hotelOfferId) {
        return new HotelPriceVerification(hotelOfferId, null, true, "Free cancellation up to 24h before check-in");
    }

    public static ProviderBookingConfirmation flightHold(ProviderType type) {
        return new ProviderBookingConfirmation(type, confirmationNumber(type), LocalDateTime.now().plusHours(24), true);
    }

    public static ProviderBookingConfirmation hotelHold(ProviderType type) {
        return new ProviderBookingConfirmation(type, confirmationNumber(type), LocalDateTime.now().plusHours(24), true);
    }

    public static FinalTicketConfirmation issueFlightTicket(ProviderType type, String pnrCode, int passengerCount) {
        List<String> tickets = new ArrayList<>();
        for (int i = 0; i < Math.max(1, passengerCount); i++) {
            tickets.add(eTicketNumber(type));
        }
        return new FinalTicketConfirmation(type, pnrCode, tickets, true);
    }

    public static FinalHotelConfirmation confirmHotelBooking(ProviderType type, String hotelBookingRef) {
        return new FinalHotelConfirmation(type, hotelBookingRef, confirmationNumber(type), true);
    }

    private static String confirmationNumber(ProviderType type) {
        return type.name().substring(0, 3) + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static String eTicketNumber(ProviderType type) {
        return "ETK-" + type.name().charAt(0) + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private static Random seeded(String... parts) {
        return new Random(String.join("|", parts).hashCode());
    }
}
