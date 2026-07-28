package com.guentours.geo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelCityAdminServiceTest {

    @Mock
    private HotelCityRepository repository;

    private HotelCityAdminService service;

    @BeforeEach
    void setUp() {
        service = new HotelCityAdminService(repository);
        lenient().when(repository.save(any(HotelCity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createRejectsADuplicateCityAndCountry() {
        when(repository.findByCityNameIgnoreCaseAndCountryNameIgnoreCase("Douala", "Cameroun"))
                .thenReturn(Optional.of(new HotelCity("Douala", "Cameroun", 4.05, 9.7)));

        HotelCityUpsertRequest req = new HotelCityUpsertRequest("Douala", "Cameroun", 4.05, 9.7);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("existe déjà");

        verify(repository, never()).save(any());
    }

    @Test
    void createSavesANewCity() {
        when(repository.findByCityNameIgnoreCaseAndCountryNameIgnoreCase("Douala", "Cameroun"))
                .thenReturn(Optional.empty());

        HotelCity saved = service.create(new HotelCityUpsertRequest("Douala", "Cameroun", 4.05, 9.7));

        assertThat(saved.getCityName()).isEqualTo("Douala");
        assertThat(saved.getCountryName()).isEqualTo("Cameroun");
        assertThat(saved.getLatitude()).isEqualTo(4.05);
    }

    @Test
    void updateRejectsWhenAnotherCityAlreadyHasThatNameAndCountry() {
        HotelCity existing = new HotelCity("Yaounde", "Cameroun", 3.86, 11.5);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByCityNameIgnoreCaseAndCountryNameIgnoreCaseAndIdNot("Douala", "Cameroun", 1L))
                .thenReturn(true);

        HotelCityUpsertRequest req = new HotelCityUpsertRequest("Douala", "Cameroun", 4.05, 9.7);

        assertThatThrownBy(() -> service.update(1L, req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("existe déjà");

        verify(repository, never()).save(any());
    }

    @Test
    void updateRefreshesTheExistingCityInPlace() {
        HotelCity existing = new HotelCity("Duala", "Cameroun", 0.0, 0.0);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByCityNameIgnoreCaseAndCountryNameIgnoreCaseAndIdNot("Douala", "Cameroun", 1L))
                .thenReturn(false);

        HotelCity updated = service.update(1L, new HotelCityUpsertRequest("Douala", "Cameroun", 4.05, 9.7));

        assertThat(updated).isSameAs(existing);
        assertThat(updated.getCityName()).isEqualTo("Douala");
        assertThat(updated.getLatitude()).isEqualTo(4.05);
        assertThat(updated.getLongitude()).isEqualTo(9.7);
    }

    @Test
    void updateThrowsNotFoundForAnUnknownId() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, new HotelCityUpsertRequest("Douala", "Cameroun", 4.05, 9.7)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("introuvable");
    }

    @Test
    void deleteThrowsNotFoundForAnUnknownId() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("introuvable");

        verify(repository, never()).deleteById(any());
    }

    @Test
    void deleteRemovesAnExistingCity() {
        when(repository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void listWithoutAQueryReturnsEverySortedAndPaginated() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("cityName"));
        Page<HotelCity> page = new PageImpl<>(List.of(new HotelCity("Douala", "Cameroun", 4.05, 9.7)));
        when(repository.findAll(pageable)).thenReturn(page);

        Page<HotelCity> result = service.list(null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(repository, never())
                .findByCityNameContainingIgnoreCaseOrCountryNameContainingIgnoreCase(any(), any(), any());
    }

    @Test
    void listWithAQueryDelegatesToTheSubstringSearch() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<HotelCity> page = new PageImpl<>(List.of(new HotelCity("Douala", "Cameroun", 4.05, 9.7)));
        when(repository.findByCityNameContainingIgnoreCaseOrCountryNameContainingIgnoreCase("dou", "dou", pageable))
                .thenReturn(page);

        Page<HotelCity> result = service.list("dou", pageable);

        assertThat(result.getContent()).extracting(HotelCity::getCityName).containsExactly("Douala");
        verify(repository, never()).findAll(eq(pageable));
    }
}
