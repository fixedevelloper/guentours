package com.guentours.commission;

import com.guentours.shared.Money;
import com.guentours.shared.exception.BusinessException;
import com.guentours.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ShareholderService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ShareholderRepository shareholderRepository;
    private final ShareholderCommissionEntryRepository entryRepository;

    ShareholderService(ShareholderRepository shareholderRepository,
                       ShareholderCommissionEntryRepository entryRepository) {
        this.shareholderRepository = shareholderRepository;
        this.entryRepository = entryRepository;
    }

    public List<Shareholder> findAll() {
        return shareholderRepository.findAll();
    }

    public List<Money> balanceFor(String shareholderId) {
        return entryRepository.totalsByShareholder(shareholderId).stream()
                .map(t -> new Money(t.getTotal(), t.getCurrency()))
                .toList();
    }

    @Transactional
    public Shareholder create(String name, BigDecimal percentage) {
        validatePercentage(percentage, null);
        return shareholderRepository.save(new Shareholder(name, percentage));
    }

    @Transactional
    public Shareholder update(String id, String name, BigDecimal percentage, Boolean active) {
        Shareholder shareholder = shareholderRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Actionnaire introuvable : " + id));
        if (percentage != null) {
            validatePercentage(percentage, id);
            shareholder.setPercentage(percentage);
        }
        if (name != null) {
            shareholder.setName(name);
        }
        if (active != null) {
            shareholder.setActive(active);
        }
        return shareholder;
    }

    /** Rejects a percentage outside 0-100, or one that would push the active total past 100%. */
    private void validatePercentage(BigDecimal percentage, String excludingShareholderId) {
        if (percentage.compareTo(ZERO) < 0 || percentage.compareTo(HUNDRED) > 0) {
            throw new BusinessException("Le pourcentage doit être compris entre 0 et 100.");
        }
        BigDecimal othersTotal = shareholderRepository.findByActiveTrue().stream()
                .filter(s -> excludingShareholderId == null || !excludingShareholderId.equals(s.getId()))
                .map(Shareholder::getPercentage)
                .reduce(ZERO, BigDecimal::add);
        if (othersTotal.add(percentage).compareTo(HUNDRED) > 0) {
            throw new BusinessException(
                    "La somme des pourcentages des actionnaires actifs dépasserait 100%% (déjà %s%%)."
                            .formatted(othersTotal));
        }
    }
}
