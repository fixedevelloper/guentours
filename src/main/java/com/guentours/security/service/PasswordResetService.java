package com.guentours.security.service;

import com.guentours.shared.exception.BusinessException;
import com.guentours.user.domain.PasswordResetToken;
import com.guentours.user.domain.PasswordResetTokenRepository;
import com.guentours.user.domain.UserRepository;
import com.guentours.user.event.PasswordResetRequestedEvent;
import com.guentours.user.service.PendingPasswordResetLinkSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class PasswordResetService implements PendingPasswordResetLinkSource {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ResetLinkCache resetLinkCache = new ResetLinkCache();
    private final ForgotPasswordEmailLimiter emailLimiter = new ForgotPasswordEmailLimiter();
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void requestReset(String email) {
                String normalizedEmail = email.trim().toLowerCase();
                if (!emailLimiter.tryConsume(normalizedEmail)) {
                        log.info("Limite de demandes de reset atteinte pour {}", maskEmail(email));
                        return; // silencieux — même comportement que l'email inexistant, aucune info exposée
                }
        userRepository.findByEmailIgnoreCase(normalizedEmail).ifPresent(user -> {
            String rawToken = generateRawToken();
            String tokenHash = hash(rawToken);

            tokenRepository.save(new PasswordResetToken(
                    tokenHash,
                    user.getId(),
                    Instant.now().plus(TOKEN_TTL)
            ));

            String resetLink = "https://guenstravel.com/reset-password?token=" + rawToken; // ⚠️ adapte le domaine/route
            resetLinkCache.store(user.getId(), resetLink);

            eventPublisher.publishEvent(new PasswordResetRequestedEvent(user.getId()));
        });

        log.info("Demande de reset mot de passe traitée pour {}", maskEmail(email));
    }

    /**
     * Usage unique — appelé par le listener de notification pour récupérer le lien
     * à envoyer par email. Le lien n'est jamais transporté dans l'event lui-même.
     * Expire après 5 minutes si non consommé (ex: échec transitoire de publication de l'event).
     */
    @Override
    public Optional<String> consumePendingResetLink(String userId) {
        return resetLinkCache.consume(userId);
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String tokenHash = hash(rawToken);

        PasswordResetToken token = tokenRepository.findByTokenHash(tokenHash)
                .filter(PasswordResetToken::isValid)
                .orElseThrow(() -> new BusinessException("Token invalide ou expiré"));

        var user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BusinessException("Utilisateur introuvable"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.markUsed();
        tokenRepository.save(token);

        log.info("Mot de passe réinitialisé pour l'utilisateur {}", user.getId());
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest(rawToken.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }
}