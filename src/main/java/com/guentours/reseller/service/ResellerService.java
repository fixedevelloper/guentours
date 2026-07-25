package com.guentours.reseller.service;

import com.guentours.reseller.domain.Reseller;
import com.guentours.reseller.domain.ResellerRepository;
import com.guentours.reseller.domain.ResellerStatus;
import com.guentours.reseller.web.ResellerRegistrationRequest;
import com.guentours.storage.StorageService;
import com.guentours.user.domain.Role;
import com.guentours.user.domain.User;
import com.guentours.user.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResellerService {

    private static final String PROMO_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("0.05");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ResellerRepository repository;
    private final UserRepository userRepository; // Directement injecté pour gérer les rôles et la validation
    private final StorageService fileStorageService;

    /**
     * Enregistre une nouvelle demande d'adhésion revendeur avec téléversement optionnel du logo sur MinIO.
     */
    @Transactional
    public Reseller register(ResellerRegistrationRequest req, MultipartFile logoFile) {
        if (repository.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un compte existe déjà avec cet email.");
        }

        // Vérification de l'existence de l'utilisateur pour éviter les violations de clé étrangère
        if (req.userId() != null && !userRepository.existsById(req.userId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable avec l'ID spécifié.");
        }

        // 1. Upload du logo sur MinIO si présent, sinon conservation de req.logoUrl()
        String logoUrl = req.logoUrl();
        if (logoFile != null && !logoFile.isEmpty()) {
            logoUrl = fileStorageService.upload("resellers/logos", logoFile);
        }

        // 2. Génération du code promo unique
        String promoCode = generateUniquePromoCode(req.companyName());

        // 3. Instanciation de l'entité
        Reseller reseller = new Reseller(
                req.userId(),
                req.companyName(),
                req.contactName(),
                req.email(),
                req.phone(),
                req.registrationNumber(),
                req.city(),
                req.country(),
                promoCode,
                DEFAULT_COMMISSION_RATE,
                logoUrl
        );

        Reseller saved = repository.save(reseller);
        log.info("Demande d'adhésion revendeur créée [ID: {}, Company: {}, PromoCode: {}]",
                saved.getId(), saved.getCompanyName(), promoCode);
        return saved;
    }

    /**
     * Surchargé pour l'enregistrement sans fichier Multipart direct (ex: JSON avec URL déjà fournie).
     */
    @Transactional
    public Reseller register(ResellerRegistrationRequest req) {
        return register(req, null);
    }

    public Reseller findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Revendeur introuvable"));
    }

    /**
     * Valide et récupère un revendeur actif à partir de son code promo (ex: lors du checkout).
     */
    public Optional<Reseller> findActiveByPromoCode(String promoCode) {
        if (promoCode == null || promoCode.isBlank()) {
            return Optional.empty();
        }
        return repository.findByPromoCodeIgnoreCase(promoCode.trim())
                .filter(r -> r.getStatus() == ResellerStatus.APPROVED);
    }

    public Page<Reseller> findAll(ResellerStatus status, Pageable pageable) {
        return status == null ? repository.findAll(pageable) : repository.findByStatus(status, pageable);
    }

    // =========================================================================
    // Flux d'Approbation & Administration (Modifications d'état)
    // =========================================================================

    @Transactional
    public Reseller approve(String id, BigDecimal commissionRate) {
        Reseller reseller = findById(id);

        BigDecimal targetRate = (commissionRate != null) ? commissionRate :
                (reseller.getCommissionRate() != null ? reseller.getCommissionRate() : DEFAULT_COMMISSION_RATE);

        reseller.setCommissionRate(targetRate);
        reseller.approve();

        // 1. Récupération de l'utilisateur (via relation JPA ou par ID)
        User user = reseller.getUser();
        if (user == null && reseller.getUserId() != null) {
            user = userRepository.findById(reseller.getUserId()).orElse(null);
        }

        // 2. Mise à jour du rôle utilisateur vers RESELLER
        if (user != null) {

                // Ajouter le rôle à la collection existante
                user.promoteToReseller();
                userRepository.save(user);
                log.info("Rôle RESELLER ajouté à l'utilisateur [ID: {}]", user.getId());

        } else {
            log.warn("Aucun utilisateur associé au revendeur [ID: {}] lors de l'approbation", id);
        }

        Reseller updated = repository.save(reseller);
        log.info("Revendeur approuvé [ID: {}, Taux: {}%]", updated.getId(), targetRate.multiply(BigDecimal.valueOf(100)));

        return updated;
    }

    @Transactional
    public Reseller reject(String id) {
        Reseller reseller = findById(id);
        reseller.reject();

        Reseller updated = repository.save(reseller);
        log.info("Demande revendeur rejetée [ID: {}]", updated.getId());
        return updated;
    }

    @Transactional
    public Reseller suspend(String id) {
        Reseller reseller = findById(id);
        reseller.suspend();

        Reseller updated = repository.save(reseller);
        log.info("Compte revendeur suspendu [ID: {}]", updated.getId());
        return updated;
    }

    // =========================================================================
    // Utilitaire : Génération de Code Promo Unique
    // =========================================================================

    private String generateUniquePromoCode(String companyName) {
        String base = sanitizeName(companyName);
        if (base.length() < 4) {
            base = (base + "PROMO").substring(0, 4);
        } else {
            base = base.substring(0, 4);
        }

        String candidate;
        int attempts = 0;
        do {
            StringBuilder suffix = new StringBuilder(4);
            for (int i = 0; i < 4; i++) {
                suffix.append(PROMO_ALPHABET.charAt(RANDOM.nextInt(PROMO_ALPHABET.length())));
            }
            candidate = base + suffix;
            attempts++;

            if (attempts > 100) {
                // Secours si un préfixe saturé génère trop de collisions
                base = "G" + PROMO_ALPHABET.charAt(RANDOM.nextInt(PROMO_ALPHABET.length())) + "TR";
            }
        } while (repository.existsByPromoCode(candidate));

        return candidate;
    }

    private String sanitizeName(String input) {
        if (input == null) return "GT";
        // Supprime les accents et caractères spéciaux
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }
}