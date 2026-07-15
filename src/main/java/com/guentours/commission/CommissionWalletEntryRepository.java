package com.guentours.commission;

import org.springframework.data.jpa.repository.JpaRepository;

interface CommissionWalletEntryRepository extends JpaRepository<CommissionWalletEntry, String> {
}
