package com.guentours.search.service;

import com.guentours.geo.HotelCityRepository;
import com.guentours.provider.*;
import com.guentours.search.OfferCache;
import com.guentours.search.domain.HotelHarmonizer;
import com.guentours.search.domain.HotelSearchResult;
import com.guentours.search.web.HotelSearchRequest;
import com.guentours.shared.Money;
import com.guentours.shared.exception.NotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelSearchServiceTest {

    @Mock
    private TravelProviderClient travelport;
    @Mock
    private TravelProviderClient travelopro;
    @Mock
    private HotelHarmonizer harmonizer;
    @Mock
    private OfferCache offerCache;
    @Mock
    private HotelCityRepository hotelCityRepository;

    private ExecutorService executor;
    private HotelSearchService service;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
        lenient().when(travelport.getType()).thenReturn(ProviderType.TRAVELPORT);
        lenient().when(travelopro.getType()).thenReturn(ProviderType.TRAVELOPRO);
        lenient().when(hotelCityRepository.search(any(), any())).thenReturn(List.of());
        service = new HotelSearchService(List.of(travelport, travelopro), executor, harmonizer, offerCache,
                hotelCityRepository);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    private HotelOffer offer(ProviderType type, String searchIdentifier) {
        Map<String, String> context = searchIdentifier == null ? Map.of() : Map.of("searchIdentifier", searchIdentifier);
        return new HotelOffer(type, "offer-1", "Test Hotel", "PAR", "Standard",
                LocalDate.now().plusDays(30), LocalDate.now().plusDays(34),
                new Money(BigDecimal.valueOf(100), "XAF"), 4.0, null, context);
    }

    @Test
    void searchSkipsDisabledProvidersAndCapturesOnlyIdentifiersThatWereReturned() {
        when(travelport.isEnabled()).thenReturn(true);
        when(travelopro.isEnabled()).thenReturn(false);
        when(travelport.searchHotels(any())).thenReturn(List.of(offer(ProviderType.TRAVELPORT, "search-123")));
        when(harmonizer.harmonize(any())).thenReturn(List.of());
        when(offerCache.cacheHotelSearchSession(any(), any())).thenReturn("session-abc");

        HotelSearchResult result = service.search(request());

        verify(travelopro, never()).searchHotels(any());
        verify(travelport).searchHotels(any());
        verify(offerCache).cacheHotelSearchSession(any(), eq(Map.of(ProviderType.TRAVELPORT, "search-123")));
        assertThat(result.searchId()).isEqualTo("session-abc");
    }

    @Test
    void searchLeavesSearchIdNullWhenNoProviderCapturesAnIdentifier() {
        when(travelport.isEnabled()).thenReturn(true);
        when(travelopro.isEnabled()).thenReturn(true);
        when(travelport.searchHotels(any())).thenReturn(List.of(offer(ProviderType.TRAVELPORT, null)));
        when(travelopro.searchHotels(any())).thenReturn(List.of(offer(ProviderType.TRAVELOPRO, null)));
        when(harmonizer.harmonize(any())).thenReturn(List.of());

        HotelSearchResult result = service.search(request());

        verify(offerCache, never()).cacheHotelSearchSession(any(), any());
        assertThat(result.searchId()).isNull();
    }

    @Test
    void loadMoreThrowsNotFoundWhenTheSessionHasExpired() {
        when(offerCache.getHotelSearchSession("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadMore("missing", 2))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void loadMoreOnlyCallsProvidersThatHaveACapturedIdentifier() {
        HotelSearchCriteria criteria = anyCriteria();
        OfferCache.HotelSearchSession session = new OfferCache.HotelSearchSession(
                criteria, Map.of(ProviderType.TRAVELPORT, "search-123"));
        when(offerCache.getHotelSearchSession("session-abc")).thenReturn(Optional.of(session));
        when(travelport.isEnabled()).thenReturn(true);
        when(travelport.loadMoreHotels(criteria, "search-123", 2))
                .thenReturn(List.of(offer(ProviderType.TRAVELPORT, "search-123")));
        when(harmonizer.harmonize(any())).thenReturn(List.of());

        service.loadMore("session-abc", 2);

        verify(travelport).loadMoreHotels(criteria, "search-123", 2);
        verify(travelopro, never()).loadMoreHotels(any(), any(), anyInt());
    }

    private HotelSearchRequest request() {
        return new HotelSearchRequest("PAR", LocalDate.now().plusDays(30), LocalDate.now().plusDays(34), 1, 1, "XAF");
    }

    private HotelSearchCriteria anyCriteria() {
        return new HotelSearchCriteria("PAR", LocalDate.now().plusDays(30), LocalDate.now().plusDays(34), 1, 1, "XAF", null, null);
    }
}
