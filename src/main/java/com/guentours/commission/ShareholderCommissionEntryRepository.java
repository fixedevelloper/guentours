package com.guentours.commission;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

interface ShareholderCommissionEntryRepository extends JpaRepository<ShareholderCommissionEntry, String> {

    @Query("""
            select e.amount.currency as currency, sum(e.amount.amount) as total
            from ShareholderCommissionEntry e
            where e.shareholderId = :shareholderId
            group by e.amount.currency
            """)
    List<CurrencyTotal> totalsByShareholder(@Param("shareholderId") String shareholderId);

    interface CurrencyTotal {
        String getCurrency();

        BigDecimal getTotal();
    }
}
