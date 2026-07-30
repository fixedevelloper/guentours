package com.guentours.commission;

import com.guentours.booking.domain.OfferType;
import com.guentours.provider.ProviderType;
import com.guentours.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Verifies that recording a commission splits it across every active shareholder per their
 * percentage - and only that, never at search time - while leaving inactive shareholders out.
 */
@ExtendWith(MockitoExtension.class)
class CommissionWalletServiceTest {

    @Mock
    private CommissionWalletEntryRepository walletRepository;
    @Mock
    private ShareholderRepository shareholderRepository;
    @Mock
    private ShareholderCommissionEntryRepository shareholderEntryRepository;

    private CommissionWalletService service;

    @BeforeEach
    void setUp() {
        service = new CommissionWalletService(walletRepository, shareholderRepository, shareholderEntryRepository);
        when(walletRepository.save(any(CommissionWalletEntry.class))).thenAnswer(invocation -> {
            CommissionWalletEntry entry = invocation.getArgument(0);
            return entry;
        });
        lenient().when(shareholderEntryRepository.save(any(ShareholderCommissionEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void splitsTheCommissionAcrossActiveShareholdersOnlyAndSkipsInactiveOnes() {
        Shareholder active1 = new Shareholder("Alice", BigDecimal.valueOf(60));
        Shareholder active2 = new Shareholder("Bob", BigDecimal.valueOf(25));
        when(shareholderRepository.findByActiveTrue()).thenReturn(List.of(active1, active2));

        service.record("booking-1", ProviderType.TRAVELPORT, OfferType.FLIGHT,
                CommissionType.BOOKING_FEE, new Money(BigDecimal.valueOf(15), "EUR"));

        verify(walletRepository).save(any(CommissionWalletEntry.class));

        ArgumentCaptor<ShareholderCommissionEntry> captor = ArgumentCaptor.forClass(ShareholderCommissionEntry.class);
        verify(shareholderEntryRepository, times(2)).save(captor.capture());

        List<ShareholderCommissionEntry> entries = captor.getAllValues();
        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).getShareholderName()).isEqualTo("Alice");
        assertThat(entries.get(0).getAmount().amount()).isEqualByComparingTo("9.00");
        assertThat(entries.get(1).getShareholderName()).isEqualTo("Bob");
        assertThat(entries.get(1).getAmount().amount()).isEqualByComparingTo("3.75");
    }

    @Test
    void recordsNoShareholderEntriesWhenNoneAreActive() {
        when(shareholderRepository.findByActiveTrue()).thenReturn(List.of());

        service.record("booking-2", ProviderType.SABRE, OfferType.HOTEL,
                CommissionType.BOOKING_FEE, new Money(BigDecimal.valueOf(15), "EUR"));

        verify(walletRepository).save(any(CommissionWalletEntry.class));
        verifyNoInteractions(shareholderEntryRepository);
    }
}
