package com.guentours.commission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface ShareholderRepository extends JpaRepository<Shareholder, String> {

    List<Shareholder> findByActiveTrue();
}
