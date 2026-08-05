package com.guentours.security;

import com.guentours.user.domain.PasswordResetTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PasswordResetTokenCleanup {

    private final PasswordResetTokenRepository tokenRepository;

    public PasswordResetTokenCleanup(PasswordResetTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Scheduled(cron = "0 0 3 * * *") // tous les jours à 3h du matin
    public void cleanup() {
        tokenRepository.deleteAllExpiredBefore(Instant.now());
    }
}
