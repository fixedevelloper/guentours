package com.guentours.user.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    // Référence externe vers Partner (module partners) — pas de FK inter-module
    private String partnerId;

    @Column(nullable = false)
    private boolean autoProvisioned = false;

    @Column(nullable = false)
    private boolean mustChangePassword = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected User() {}

    /** Compte client : inscription directe ou auto-provisioning au checkout. */
    public User(String email, String fullName, String phone, String passwordHash, boolean autoProvisioned) {
        this.email = email;
        this.fullName = fullName;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = Role.CUSTOMER;
        this.autoProvisioned = autoProvisioned;
        this.mustChangePassword = autoProvisioned; // mot de passe généré → à changer à la 1ère connexion
    }

    /** Compte partenaire, créé à l'approbation du dossier partenaire. */
    public User(String email, String passwordHash, String fullName, Role role, String partnerId) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.partnerId = partnerId;
    }

    public void setMustChangePassword(boolean value) { this.mustChangePassword = value; }

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public String getPartnerId() { return partnerId; }
    public boolean isAutoProvisioned() { return autoProvisioned; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public Instant getCreatedAt() { return createdAt; }
}