package com.guentours.destination;

import com.guentours.booking.domain.BookingRepository;
import com.guentours.booking.domain.DestinationBookingCount;
import com.guentours.geo.Airport;
import com.guentours.geo.AirportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeaturedDestinationServiceTest {

    @Mock
    private FeaturedDestinationRepository repository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private AirportRepository airportRepository;

    private FeaturedDestinationService service;

    @BeforeEach
    void setUp() {
        service = new FeaturedDestinationService(repository, bookingRepository, airportRepository);
        lenient().when(repository.save(any(FeaturedDestination.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createRejectsADuplicateDestinationCode() {
        when(repository.findByDestinationCodeIgnoreCase("CDG"))
                .thenReturn(Optional.of(new FeaturedDestination("Paris", "France", "CDG", null, 0, true)));

        var req = new FeaturedDestinationUpsertRequest("Paris", "France", "cdg", null, 0, true);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("existe déjà");

        verify(repository, never()).save(any());
    }

    @Test
    void createSavesANewDestinationWithAnUppercasedCode() {
        when(repository.findByDestinationCodeIgnoreCase("CDG")).thenReturn(Optional.empty());

        FeaturedDestination saved = service.create(new FeaturedDestinationUpsertRequest("Paris", "France", "cdg", null, 0, true));

        assertThat(saved.getCityName()).isEqualTo("Paris");
        assertThat(saved.getDestinationCode()).isEqualTo("CDG");
    }

    @Test
    void createAllowsNoDestinationCode() {
        FeaturedDestination saved = service.create(new FeaturedDestinationUpsertRequest("Douala", "Cameroun", null, null, 0, true));

        assertThat(saved.getDestinationCode()).isNull();
        verify(repository, never()).findByDestinationCodeIgnoreCase(any());
    }

    @Test
    void updateRejectsWhenAnotherDestinationAlreadyHasThatCode() {
        FeaturedDestination existing = new FeaturedDestination("Tokyo", "Japon", "NRT", null, 0, true);
        when(repository.findById("1")).thenReturn(Optional.of(existing));
        when(repository.existsByDestinationCodeIgnoreCaseAndIdNot("CDG", "1")).thenReturn(true);

        var req = new FeaturedDestinationUpsertRequest("Tokyo", "Japon", "CDG", null, 0, true);

        assertThatThrownBy(() -> service.update("1", req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("existe déjà");

        verify(repository, never()).save(any());
    }

    @Test
    void updateThrowsNotFoundForAnUnknownId() {
        when(repository.findById("99")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("99", new FeaturedDestinationUpsertRequest("Paris", "France", "CDG", null, 0, true)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void deleteThrowsNotFoundForAnUnknownId() {
        when(repository.existsById("99")).thenReturn(false);

        assertThatThrownBy(() -> service.delete("99"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("introuvable");

        verify(repository, never()).deleteById(any());
    }

    @Test
    void refreshFromBookingsAddsOnlyUntrackedDestinationsResolvedViaAirports() {
        DestinationBookingCount cdg = countOf("CDG", 42);
        DestinationBookingCount nrt = countOf("NRT", 30);
        when(bookingRepository.countFlightBookingsByDestination(any(Pageable.class))).thenReturn(List.of(cdg, nrt));

        // CDG already tracked (whether auto-suggested earlier or admin-added) - must not be touched again.
        when(repository.findByDestinationCodeIgnoreCase("CDG"))
                .thenReturn(Optional.of(new FeaturedDestination("Paris", "France", "CDG", "old.jpg", 0, true)));
        when(repository.findByDestinationCodeIgnoreCase("NRT")).thenReturn(Optional.empty());
        when(repository.findAllByOrderByDisplayOrderAsc())
                .thenReturn(List.of(new FeaturedDestination("Paris", "France", "CDG", "old.jpg", 0, true)));
        when(airportRepository.findById("NRT")).thenReturn(Optional.of(new Airport("NRT", "Narita", "Tokyo", "Japon")));

        int added = service.refreshFromBookings(10);

        assertThat(added).isEqualTo(1);
        verify(repository, times(1)).save(argThat(d -> "NRT".equals(d.getDestinationCode())
                && "Tokyo".equals(d.getCityName()) && "Japon".equals(d.getCountryName()) && d.getDisplayOrder() == 1));
        verify(repository, never()).save(argThat(d -> "CDG".equals(d.getDestinationCode())));
    }

    @Test
    void refreshFromBookingsFallsBackToTheCodeWhenNoAirportIsFound() {
        when(bookingRepository.countFlightBookingsByDestination(any(Pageable.class))).thenReturn(List.of(countOf("XXX", 5)));
        when(repository.findByDestinationCodeIgnoreCase("XXX")).thenReturn(Optional.empty());
        when(repository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of());
        when(airportRepository.findById("XXX")).thenReturn(Optional.empty());

        service.refreshFromBookings(10);

        verify(repository).save(argThat(d -> "XXX".equals(d.getCityName()) && d.getDisplayOrder() == 0));
    }

    private DestinationBookingCount countOf(String code, long count) {
        return new DestinationBookingCount() {
            @Override
            public String getDestinationCode() {
                return code;
            }

            @Override
            public long getBookingCount() {
                return count;
            }
        };
    }
}
