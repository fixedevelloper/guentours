package com.guentours.commission;

import com.guentours.booking.OfferType;
import com.guentours.provider.ProviderType;
import com.guentours.shared.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommissionWalletService {

    private final CommissionWalletEntryRepository repository;

    CommissionWalletService(CommissionWalletEntryRepository repository) {
        this.repository = repository;
    }

    /** Records a commission earned on a booking (booking fee at checkout, or reservation fee when paid). */
    @Transactional
    public void record(String bookingId, ProviderType providerType, OfferType offerType,
                       CommissionType commissionType, Money amount) {
        repository.save(new CommissionWalletEntry(bookingId, providerType, offerType, commissionType, amount));
    }

    /** Cumulative commission earned so far, grouped by currency. */
    public List<Money> totalBalance() {
        Map<String, BigDecimal> sums = new LinkedHashMap<>();
        for (CommissionWalletEntry entry : repository.findAll()) {
            sums.merge(entry.getAmount().currency(), entry.getAmount().amount(), BigDecimal::add);
        }
        return sums.entrySet().stream()
                .map(e -> new Money(e.getValue(), e.getKey()))
                .toList();
    }

    public long entryCount() {
        return repository.count();
    }
}
