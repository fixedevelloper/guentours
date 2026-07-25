package com.guentours.reseller.web;

import com.guentours.reseller.domain.*;
import com.guentours.reseller.service.ResellerCommissionService;
import com.guentours.reseller.service.ResellerService;
import com.guentours.reseller.service.ResellerWithdrawalService;
import com.guentours.reseller.service.ResellerBookingService; // Service gérant les réservations
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/resellers")
@RequiredArgsConstructor
public class ResellerController {

    private final ResellerService service;
    private final ResellerWithdrawalService withdrawalService;
    private final ResellerCommissionService commissionService;
    private final ResellerCommissionRepository commissionRepository;
    private final ResellerBookingService bookingService; // Injection du service des réservations

    // =========================================================================
    // 1. Inscription & Consultation Générale
    // =========================================================================

    /**
     * Inscription standard via JSON (ex: URL de logo déjà fournie).
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ResellerResponse register(@Valid @RequestBody ResellerRegistrationRequest req) {
        return ResellerResponse.from(service.register(req));
    }

    /**
     * Inscription Multipart avec téléversement direct du logo sur MinIO.
     */
    @PostMapping(value = "/register-with-logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ResellerResponse registerWithLogo(
            @Valid @RequestPart("request") ResellerRegistrationRequest req,
            @RequestPart(value = "logo", required = false) MultipartFile logoFile) {
        return ResellerResponse.from(service.register(req, logoFile));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<ResellerResponse> list(@RequestParam(required = false) ResellerStatus status, Pageable pageable) {
        return service.findAll(status, pageable).map(ResellerResponse::from);
    }

    @GetMapping("/{id}")
    public ResellerResponse getOne(@PathVariable String id) {
        return ResellerResponse.from(service.findById(id));
    }

    /**
     * Endpoint public/checkout : Valide un code promo et vérifie que le revendeur est actif.
     */
    @GetMapping("/promo/{promoCode}")
    public ResponseEntity<ResellerResponse> getByPromoCode(@PathVariable String promoCode) {
        return service.findActiveByPromoCode(promoCode)
                .map(reseller -> ResponseEntity.ok(ResellerResponse.from(reseller)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Code promo invalide ou expiré"));
    }

    // =========================================================================
    // 2. Modération & Administration Revendeurs
    // =========================================================================

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResellerResponse approve(@PathVariable String id, @Valid @RequestBody ResellerApprovalRequest req) {
        return ResellerResponse.from(service.approve(id, req.commissionRate()));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResellerResponse reject(@PathVariable String id) {
        return ResellerResponse.from(service.reject(id));
    }

    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResellerResponse suspend(@PathVariable String id) {
        return ResellerResponse.from(service.suspend(id));
    }

    // =========================================================================
    // 3. Réservations (Ventes), Commissions & Solde
    // =========================================================================

    /**
     * Récupère l'historique paginé des réservations / billets générés par un revendeur.
     */
    @GetMapping("/{id}/bookings")
    public Page<ResellerBookingResponse> listBookings(@PathVariable String id, Pageable pageable) {
        return bookingService.findByResellerId(id, pageable).map(ResellerBookingResponse::from);
    }

    @GetMapping("/{id}/commissions")
    public Page<ResellerCommissionResponse> commissions(@PathVariable String id, Pageable pageable) {
        return commissionRepository.findByResellerId(id, pageable).map(ResellerCommissionResponse::from);
    }

    /**
     * Consulte le solde retirable disponible en temps réel pour un revendeur.
     */
    @GetMapping("/{id}/balance")
    public ResponseEntity<Map<String, BigDecimal>> getBalance(@PathVariable String id) {
        BigDecimal withdrawableBalance = withdrawalService.getWithdrawableBalance(id);
        return ResponseEntity.ok(Map.of("withdrawableBalance", withdrawableBalance));
    }

    // =========================================================================
    // 4. Retraits & Virements (Espace Revendeur & Admin)
    // =========================================================================

    /**
     * Demande un retrait de commission (Côté Revendeur).
     */
    @PostMapping("/{id}/withdrawals")
    @ResponseStatus(HttpStatus.CREATED)
    public ResellerWithdrawalResponse requestWithdrawal(
            @PathVariable String id,
            @Valid @RequestBody ResellerWithdrawalRequest req) {
        return ResellerWithdrawalResponse.from(withdrawalService.requestWithdrawal(id, req));
    }

    /**
     * Historique des demandes de retrait d'un revendeur donné.
     */
    @GetMapping("/{id}/withdrawals")
    public Page<ResellerWithdrawalResponse> getResellerWithdrawals(@PathVariable String id, Pageable pageable) {
        return withdrawalService.findByResellerId(id, pageable).map(ResellerWithdrawalResponse::from);
    }

    /**
     * Liste globale de toutes les demandes de retrait (Côté Admin).
     */
    @GetMapping("/withdrawals")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<ResellerWithdrawalResponse> listWithdrawals(
            @RequestParam(required = false) ResellerWithdrawalStatus status,
            Pageable pageable) {
        return withdrawalService.findAll(status, pageable).map(ResellerWithdrawalResponse::from);
    }

    /**
     * Exécute et marque un virement comme payé (Côté Admin).
     */
    @PatchMapping("/withdrawals/{withdrawalId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResellerWithdrawalResponse approveWithdrawal(
            @PathVariable String withdrawalId,
            @Valid @RequestBody WithdrawalApprovalRequest req) {
        return ResellerWithdrawalResponse.from(
                withdrawalService.approve(withdrawalId)
        );
    }

    /**
     * Rejette une demande de retrait (Côté Admin).
     */
    @PatchMapping("/withdrawals/{withdrawalId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResellerWithdrawalResponse rejectWithdrawal(
            @PathVariable String withdrawalId,
            @Valid @RequestBody WithdrawalRejectionRequest req) {
        return ResellerWithdrawalResponse.from(withdrawalService.reject(withdrawalId, req.reason()));
    }
}