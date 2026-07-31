package com.guentours.commission;

import com.guentours.shared.Money;
import com.guentours.shared.exception.BusinessException;
import com.guentours.shared.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShareholderServiceTest {

    @Mock
    private ShareholderRepository shareholderRepository;
    @Mock
    private ShareholderCommissionEntryRepository entryRepository;

    private ShareholderService service;

    @BeforeEach
    void setUp() {
        service = new ShareholderService(shareholderRepository, entryRepository);
        lenient().when(shareholderRepository.save(any(Shareholder.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createRejectsAPercentageOutsideZeroToHundred() {
        assertThatThrownBy(() -> service.create("Alice", BigDecimal.valueOf(-1)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.create("Alice", BigDecimal.valueOf(101)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createRejectsAPercentageThatWouldPushActiveTotalPastHundred() {
        Shareholder existing = new Shareholder("Alice", BigDecimal.valueOf(70));
        when(shareholderRepository.findByActiveTrue()).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.create("Bob", BigDecimal.valueOf(31)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createAcceptsAPercentageThatFitsUnderTheRemainingRoom() {
        Shareholder existing = new Shareholder("Alice", BigDecimal.valueOf(70));
        when(shareholderRepository.findByActiveTrue()).thenReturn(List.of(existing));

        Shareholder created = service.create("Bob", BigDecimal.valueOf(30));

        assertThat(created.getName()).isEqualTo("Bob");
        assertThat(created.getPercentage()).isEqualByComparingTo("30");
    }

    @Test
    void updateExcludesTheShareholderItselfFromTheHundredPercentCheck() {
        Shareholder alice = new Shareholder("Alice", BigDecimal.valueOf(70));
        setId(alice, "alice-id");
        when(shareholderRepository.findById("alice-id")).thenReturn(Optional.of(alice));
        when(shareholderRepository.findByActiveTrue()).thenReturn(List.of(alice));

        Shareholder updated = service.update("alice-id", null, BigDecimal.valueOf(85), null);

        assertThat(updated.getPercentage()).isEqualByComparingTo("85");
    }

    @Test
    void updateThrowsWhenTheShareholderDoesNotExist() {
        when(shareholderRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("missing", "New name", null, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void recordPaymentSplitSplitsTheSuccessfulPaymentAcrossActiveShareholdersOnlyAndSkipsInactiveOnes() {
        Shareholder active1 = new Shareholder("Alice", BigDecimal.valueOf(60));
        Shareholder active2 = new Shareholder("Bob", BigDecimal.valueOf(25));
        when(shareholderRepository.findByActiveTrue()).thenReturn(List.of(active1, active2));
        when(entryRepository.save(any(ShareholderCommissionEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.recordPaymentSplit("payment-1", new Money(BigDecimal.valueOf(15), "EUR"));

        ArgumentCaptor<ShareholderCommissionEntry> captor = ArgumentCaptor.forClass(ShareholderCommissionEntry.class);
        verify(entryRepository, times(2)).save(captor.capture());

        List<ShareholderCommissionEntry> entries = captor.getAllValues();
        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).getPaymentId()).isEqualTo("payment-1");
        assertThat(entries.get(0).getShareholderName()).isEqualTo("Alice");
        assertThat(entries.get(0).getAmount().amount()).isEqualByComparingTo("9.00");
        assertThat(entries.get(1).getShareholderName()).isEqualTo("Bob");
        assertThat(entries.get(1).getAmount().amount()).isEqualByComparingTo("3.75");
    }

    @Test
    void recordPaymentSplitRecordsNothingWhenNoShareholderIsActive() {
        when(shareholderRepository.findByActiveTrue()).thenReturn(List.of());

        service.recordPaymentSplit("payment-2", new Money(BigDecimal.valueOf(15), "EUR"));

        verifyNoInteractions(entryRepository);
    }

    private void setId(Shareholder shareholder, String id) {
        try {
            var field = Shareholder.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(shareholder, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
