package com.guentours.search;

import com.guentours.provider.FlightOffer;
import com.guentours.provider.ProviderType;
import com.guentours.search.domain.FlightHarmonizer;
import com.guentours.search.domain.HarmonizedFlightOffer;
import com.guentours.shared.CommissionPolicy;
import com.guentours.shared.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlightHarmonizerTest {

    private final FlightHarmonizer harmonizer =
            new FlightHarmonizer(new OfferCache(), new CommissionPolicy(BigDecimal.valueOf(0.15), BigDecimal.valueOf(0.15),
                    BigDecimal.valueOf(0.15), BigDecimal.valueOf(0.15)));

    @Test
    void mergesSameFlightQuotedByMultipleProvidersAndKeepsTheCheapestPrice() {
        LocalDateTime departure = LocalDateTime.of(2026, 8, 1, 10, 0);
        LocalDateTime arrival = departure.plusHours(2);

        FlightOffer travelopro = new FlightOffer(ProviderType.TRAVELOPRO, "TO-1", "AF", "AF123", "CDG", "JFK",
                departure, arrival, "ECONOMY", new Money(BigDecimal.valueOf(400), "EUR"), 5);
        FlightOffer sabre = new FlightOffer(ProviderType.SABRE, "SB-1", "AF", "AF123", "CDG", "JFK",
                departure, arrival, "ECONOMY", new Money(BigDecimal.valueOf(350), "EUR"), 3);
        FlightOffer travelport = new FlightOffer(ProviderType.TRAVELPORT, "TP-1", "AF", "AF123", "CDG", "JFK",
                departure, arrival, "ECONOMY", new Money(BigDecimal.valueOf(420), "EUR"), 2);
        FlightOffer exclusive = new FlightOffer(ProviderType.TRAVELPORT, "TP-2", "DL", "DL999", "CDG", "JFK",
                departure, arrival, "ECONOMY", new Money(BigDecimal.valueOf(500), "EUR"), 4);

        List<HarmonizedFlightOffer> result = harmonizer.harmonize(List.of(travelopro, sabre, travelport, exclusive));

        assertThat(result).hasSize(2);

        HarmonizedFlightOffer af123 = result.stream()
                .filter(o -> o.flightNumber().equals("AF123")).findFirst().orElseThrow();
        assertThat(af123.quotes()).hasSize(3);
        assertThat(af123.quotes().get(0).price().amount()).isEqualByComparingTo("402.50");
        assertThat(af123.quotes().get(0).providerType()).isEqualTo(ProviderType.SABRE);

        HarmonizedFlightOffer dl999 = result.stream()
                .filter(o -> o.flightNumber().equals("DL999")).findFirst().orElseThrow();
        assertThat(dl999.quotes()).hasSize(1);
    }

    @Test
    void doesNotThrowWhenProvidersQuoteDifferentCurrencies() {
        // A real Travelport response has come back priced in CNY for a search that requested USD;
        // harmonizing/sorting must not crash with Money's "Cannot compare amounts in different
        // currencies" just because providers disagree on currency.
        LocalDateTime departure = LocalDateTime.of(2026, 8, 1, 10, 0);
        LocalDateTime arrival = departure.plusHours(2);

        FlightOffer travelport = new FlightOffer(ProviderType.TRAVELPORT, "TP-1", "CA", "CA123", "PEK", "SHA",
                departure, arrival, "ECONOMY", new Money(BigDecimal.valueOf(2800), "CNY"), 5);
        FlightOffer sabre = new FlightOffer(ProviderType.SABRE, "SB-1", "CA", "CA123", "PEK", "SHA",
                departure, arrival, "ECONOMY", new Money(BigDecimal.valueOf(400), "USD"), 3);

        List<HarmonizedFlightOffer> result = harmonizer.harmonize(List.of(travelport, sabre));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).quotes()).hasSize(2);
    }
}
