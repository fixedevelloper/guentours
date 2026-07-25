package com.guentours.reseller.service;

import com.guentours.reseller.domain.*;
import com.guentours.reseller.web.ResellerWithdrawalRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResellerWithdrawalService {

    private static final BigDecimal MIN_WITHDRAWAL_AMOUNT = new BigDecimal("5000");
    private static final String DEFAULT_CURRENCY = "XAF";

    private final ResellerRepository resellerRepository;
    private final ResellerWithdrawalRepository withdrawalRepository;
    private final ResellerCommissionRepository commissionRepository;

    @Transactional
    public ResellerWithdrawal requestWithdrawal(String resellerId, ResellerWithdrawalRequest req) {
        Reseller reseller = resellerRepository.findById(resellerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Revendeur introuvable"));

        if (reseller.getStatus() != ResellerStatus.APPROVED) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Seuls les revendeurs approuvés peuvent demander un retrait."
            );
        }

        if (req.amount() == null || req.amount().compareTo(MIN_WITHDRAWAL_AMOUNT) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le montant minimum de retrait est de " + MIN_WITHDRAWAL_AMOUNT
            );
        }

        if (withdrawalRepository.existsByResellerIdAndStatus(resellerId, ResellerWithdrawalStatus.PENDING)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Vous avez déjà une demande de retrait en attente de traitement."
            );
        }

        BigDecimal availableBalance = getWithdrawableBalance(resellerId);
        if (req.amount().compareTo(availableBalance) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("Solde insuffisant. Disponible : %s, Demandé : %s", availableBalance, req.amount())
            );
        }

        BigDecimal remainingWallet = availableBalance.subtract(req.amount());
        String currency = (req.currency() != null && !req.currency().isBlank())
                ? req.currency().toUpperCase()
                : DEFAULT_CURRENCY;

        ResellerWithdrawal withdrawal = new ResellerWithdrawal(
                resellerId,
                req.amount(),
                remainingWallet,
                currency,
                req.paymentMethod(),
                req.paymentDetails()
        );

        ResellerWithdrawal saved = withdrawalRepository.save(withdrawal);
        log.info("Demande de retrait créée [ID: {}, Reseller: {}, Montant: {} {}]",
                saved.getId(), resellerId, req.amount(), currency);

        return saved;
    }

    @Transactional
    public ResellerWithdrawal approve(String withdrawalId) {
        ResellerWithdrawal withdrawal = findById(withdrawalId);

        if (withdrawal.getStatus() != ResellerWithdrawalStatus.PENDING &&
                withdrawal.getStatus() != ResellerWithdrawalStatus.PROCESSING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Seules les demandes en attente ou en cours peuvent être approuvées."
            );
        }

        withdrawal.approve();
        ResellerWithdrawal updated = withdrawalRepository.save(withdrawal);

        log.info("Retrait approuvé et exécuté [ID: {}]", withdrawalId);
        return updated;
    }

    @Transactional
    public ResellerWithdrawal reject(String withdrawalId, String reason) {
        ResellerWithdrawal withdrawal = findById(withdrawalId);

        if (withdrawal.getStatus() != ResellerWithdrawalStatus.PENDING &&
                withdrawal.getStatus() != ResellerWithdrawalStatus.PROCESSING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Seules les demandes en attente ou en cours peuvent être rejetées."
            );
        }

        withdrawal.reject(reason);
        ResellerWithdrawal updated = withdrawalRepository.save(withdrawal);

        log.info("Demande de retrait rejetée [ID: {}, Raison: {}]", withdrawalId, reason);
        return updated;
    }

    public BigDecimal getWithdrawableBalance(String resellerId) {
        BigDecimal totalCommissions = commissionRepository
                .sumAmountByResellerIdAndStatus(resellerId, ResellerCommissionStatus.AVAILABLE)
                .orElse(BigDecimal.ZERO);

        BigDecimal pendingWithdrawals = withdrawalRepository
                .sumAmountByResellerIdAndStatus(resellerId, ResellerWithdrawalStatus.PENDING)
                .orElse(BigDecimal.ZERO);

        BigDecimal processingWithdrawals = withdrawalRepository
                .sumAmountByResellerIdAndStatus(resellerId, ResellerWithdrawalStatus.PROCESSING)
                .orElse(BigDecimal.ZERO);

        BigDecimal approvedWithdrawals = withdrawalRepository
                .sumAmountByResellerIdAndStatus(resellerId, ResellerWithdrawalStatus.APPROVED)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalEngaged = pendingWithdrawals.add(processingWithdrawals).add(approvedWithdrawals);
        BigDecimal netBalance = totalCommissions.subtract(totalEngaged);

        return netBalance.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : netBalance;
    }

    public ResellerWithdrawal findById(String id) {
        return withdrawalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande de retrait introuvable"));
    }

    public Page<ResellerWithdrawal> findByResellerId(String resellerId, Pageable pageable) {
        return withdrawalRepository.findByResellerId(resellerId, pageable);
    }

    public Page<ResellerWithdrawal> findAll(ResellerWithdrawalStatus status, Pageable pageable) {
        return status == null
                ? withdrawalRepository.findAll(pageable)
                : withdrawalRepository.findByStatus(status, pageable);
    }
}