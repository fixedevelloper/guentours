package com.guentours.user.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    /**
     * Référence externe vers Partner (module partners) — pas de FK inter-module
     */
    @Column(name = "partner_id", length = 36)
    private String partnerId;

    /**
     * Référence externe vers Reseller (module reseller) — pas de FK inter-module
     */
    @Column(name = "reseller_id", length = 36)
    private String resellerId;

    @Column(name = "auto_provisioned", nullable = false)
    private boolean autoProvisioned = false;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected User() {}

    /**
     * Compte client : Inscription directe ou auto-provisioning au checkout.
     */
    public User(String email, String fullName, String phone, String passwordHash, boolean autoProvisioned) {
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = Role.CUSTOMER;
        this.autoProvisioned = autoProvisioned;
        this.mustChangePassword = autoProvisioned; // Mot de passe temporaire → changement à la 1ère connexion
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Compte partenaire : Créé à l'approbation du dossier partenaire (Hôtel, Compagnie aérienne, etc.).
     */
    public User(String email, String passwordHash, String fullName, Role role, String partnerId) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.partnerId = partnerId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Compte revendeur : Créé lors de la validation du profil Revendeur.
     */
    public User(String email, String passwordHash, String fullName, String phone, String resellerId) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.phone = phone;
        this.role = Role.RESELLER;
        this.resellerId = resellerId;
        this.mustChangePassword = true; // Exige le changement de mot de passe à la première connexion
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // =========================================================================
    // Logique Métier & Modification d'état
    // =========================================================================

    public void setMustChangePassword(boolean value) {
        this.mustChangePassword = value;
    }

    public void updatePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.mustChangePassword = false;
    }

    public void linkReseller(String resellerId) {
        this.resellerId = resellerId;
        this.role = Role.RESELLER;
    }

    public void linkPartner(String partnerId, Role partnerRole) {
        this.partnerId = partnerId;
        this.role = partnerRole;
    }
    public void promoteToReseller() {
        this.role = Role.RESELLER;
    }
}