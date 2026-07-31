package com.guentours.commission;

import com.guentours.booking.domain.OfferType;
import com.guentours.provider.ProviderType;
import com.guentours.shared.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/** Verifies that recording a commission only writes the wallet entry - shareholder splits now happen on payments. */
@ExtendWith(MockitoExtension.class)
class CommissionWalletServiceTest {

    @Mock
    private CommissionWalletEntryRepository walletRepository;

    private CommissionWalletService service;

    @Test
    void recordsTheCommissionIntoTheWalletOnly() {
        service = new CommissionWalletService(walletRepository);

        service.record("booking-1", ProviderType.TRAVELPORT, OfferType.FLIGHT,
                CommissionType.BOOKING_FEE, new Money(BigDecimal.valueOf(15), "EUR"));

        verify(walletRepository).save(any(CommissionWalletEntry.class));
    }
}
