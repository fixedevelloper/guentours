package com.guentours.partner.domain;


import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "partners")
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PartnerType partnerType;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false, unique = true)
    private String registrationNumber; // RCCM / registre de commerce

    @Column(nullable = false)
    private String contactName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String country;

    private Integer fleetOrUnitsCount; // nb avions/chambres/véhicules/logements

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PartnerStatus status = PartnerStatus.PENDING_REVIEW;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant reviewedAt;

    protected Partner() {}

    public Partner(PartnerType partnerType, String companyName, String registrationNumber,
                   String contactName, String email, String phone, String city,
                   String country, Integer fleetOrUnitsCount, String description) {
        this.partnerType = partnerType;
        this.companyName = companyName;
        this.registrationNumber = registrationNumber;
        this.contactName = contactName;
        this.email = email;
        this.phone = phone;
        this.city = city;
        this.country = country;
        this.fleetOrUnitsCount = fleetOrUnitsCount;
        this.description = description;
    }

    public void approve() {
        this.status = PartnerStatus.APPROVED;
        this.reviewedAt = Instant.now();
    }

    public void reject() {
        this.status = PartnerStatus.REJECTED;
        this.reviewedAt = Instant.now();
    }

    // --- getters (pas de setters publics pour companyName/email etc. — mutation via méthodes métier) ---
    public String getId() { return id; }
    public PartnerType getPartnerType() { return partnerType; }
    public String getCompanyName() { return companyName; }
    public String getRegistrationNumber() { return registrationNumber; }
    public String getContactName() { return contactName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getCity() { return city; }
    public String getCountry() { return country; }
    public Integer getFleetOrUnitsCount() { return fleetOrUnitsCount; }
    public String getDescription() { return description; }
    public PartnerStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReviewedAt() { return reviewedAt; }
}