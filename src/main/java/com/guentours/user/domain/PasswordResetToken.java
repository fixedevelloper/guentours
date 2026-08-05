package com.guentours.user.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    // Getters
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // On stocke un hash du token, jamais le token en clair (comme un mot de passe)
    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Getter
    @Column(nullable = false)
    private String userId;

    @Getter
    @Column(nullable = false)
    private Instant expiresAt;

    @Getter
    @Column(nullable = false)
    private boolean used = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected PasswordResetToken() {
    }

    public PasswordResetToken(String tokenHash, String userId, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }

    public void markUsed() {
        this.used = true;
    }

    public String getTokenHash() { return tokenHash; }

}
