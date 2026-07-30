package com.guentours.commission;

import com.guentours.booking.domain.OfferType;
import com.guentours.provider.ProviderType;
import com.guentours.shared.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommissionWalletService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final CommissionWalletEntryRepository repository;
    private final ShareholderRepository shareholderRepository;
    private final ShareholderCommissionEntryRepository shareholderEntryRepository;

    CommissionWalletService(CommissionWalletEntryRepository repository, ShareholderRepository shareholderRepository,
                            ShareholderCommissionEntryRepository shareholderEntryRepository) {
        this.repository = repository;
        this.shareholderRepository = shareholderRepository;
        this.shareholderEntryRepository = shareholderEntryRepository;
    }

    /**
     * Records a commission earned on a booking (booking fee at checkout, or reservation fee when
     * paid), then immediately splits it across every active {@link Shareholder} per their
     * percentage - never at search time, only once the commission is actually earned. Each
     * shareholder's cut is rounded independently (same HALF_UP scale-2 rounding {@link Money}
     * itself uses), so the sum of the splits can differ from the total by a cent or two when
     * percentages don't divide evenly; that rounding remainder simply stays unassigned rather than
     * being forced onto any one shareholder.
     */
    @Transactional
    public void record(String bookingId, ProviderType providerType, OfferType offerType,
                       CommissionType commissionType, Money amount) {
        CommissionWalletEntry entry = repository.save(
                new CommissionWalletEntry(bookingId, providerType, offerType, commissionType, amount));

        for (Shareholder shareholder : shareholderRepository.findByActiveTrue()) {
            BigDecimal share = amount.amount()
                    .multiply(shareholder.getPercentage())
                    .divide(HUNDRED, 2, RoundingMode.HALF_UP);
            shareholderEntryRepository.save(new ShareholderCommissionEntry(
                    entry.getId(), shareholder.getId(), shareholder.getName(),
                    shareholder.getPercentage(), new Money(share, amount.currency())));
        }
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
