-- =================================================================
-- SCRIPT HARMONISÉ : MODULE REVENDEURS (GUENTOURS)
-- Collation & Charset : utf8mb4_unicode_ci
-- =================================================================

-- 1. TABLE DES REVENDEURS
CREATE TABLE IF NOT EXISTS resellers (
                                         id                  VARCHAR(36) NOT NULL PRIMARY KEY,
    user_id             VARCHAR(255) NULL UNIQUE, -- même longueur que users.id (VARCHAR(255))
    company_name        VARCHAR(255) NOT NULL,
    contact_name        VARCHAR(255) NOT NULL,
    email               VARCHAR(255) NOT NULL UNIQUE,
    phone               VARCHAR(30)  NOT NULL,
    promo_code          VARCHAR(20)  NOT NULL UNIQUE,
    commission_rate     DECIMAL(5, 4) NOT NULL DEFAULT 0.0500,
    registration_number VARCHAR(100) NOT NULL UNIQUE,
    city                VARCHAR(100) NOT NULL,
    country             VARCHAR(100) NOT NULL,
    logo_url            VARCHAR(512) NULL,
    wallet              DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    status              ENUM('PENDING_REVIEW', 'APPROVED', 'REJECTED', 'SUSPENDED') NOT NULL DEFAULT 'PENDING_REVIEW',
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    reviewed_at         TIMESTAMP NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. TABLE DES RETRAITS D'ARGENT
CREATE TABLE IF NOT EXISTS reseller_withdrawals (
                                                    id                  VARCHAR(36) NOT NULL PRIMARY KEY,
    reseller_id         VARCHAR(36) NOT NULL,
    amount              DECIMAL(12, 2) NOT NULL,
    remaining_wallet    DECIMAL(12, 2) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'XAF',
    payment_method      VARCHAR(50) NOT NULL,
    payment_details     TEXT NULL,
    status              ENUM('PENDING', 'PROCESSING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    rejection_reason    VARCHAR(255) NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    processed_at        TIMESTAMP NULL,
    CONSTRAINT fk_withdrawals_reseller FOREIGN KEY (reseller_id) REFERENCES resellers(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. TABLE DES COMMISSIONS
CREATE TABLE IF NOT EXISTS reseller_commission_entries (
                                                           id                  VARCHAR(36) NOT NULL PRIMARY KEY,
    reseller_id         VARCHAR(36) NOT NULL,
    booking_id          VARCHAR(255) NOT NULL,
    payout_id           VARCHAR(255) NULL,
    booking_amount      DECIMAL(12, 2) NOT NULL,
    commission_rate     DECIMAL(5, 4) NOT NULL,
    amount              DECIMAL(12, 2) NOT NULL,
    currency            VARCHAR(3) NOT NULL DEFAULT 'XAF',
    status              ENUM('PENDING', 'AVAILABLE', 'CANCELLED', 'PAID') NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_commissions_reseller FOREIGN KEY (reseller_id) REFERENCES resellers(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. ALIGNEMENT DES TABLES EXISTANTES (USERS & BOOKINGS)
-- Statements séparés : mélanger CONVERT TO CHARACTER SET avec ADD COLUMN/MODIFY COLUMN
-- dans un seul ALTER TABLE peut ne pas s'appliquer correctement selon la version MySQL.

ALTER TABLE users
    CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE users
    ADD COLUMN reseller_id VARCHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
    ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE users
    MODIFY COLUMN role ENUM(
    'ADMIN',
    'CUSTOMER',
    'RESELLER',
    'PARTNER_AIRLINE',
    'PARTNER_HOTEL',
    'PARTNER_CAR_RENTAL',
    'PARTNER_FURNISHED_RENTAL'
    ) NOT NULL DEFAULT 'CUSTOMER';


ALTER TABLE bookings
    ADD COLUMN reseller_id VARCHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL;

-- 5. AJOUT SÉCURISÉ DES CLÉS ÉTRANGÈRES
ALTER TABLE users
    ADD CONSTRAINT fk_users_reseller FOREIGN KEY (reseller_id) REFERENCES resellers(id) ON DELETE SET NULL;

ALTER TABLE bookings
    ADD CONSTRAINT fk_bookings_reseller FOREIGN KEY (reseller_id) REFERENCES resellers(id) ON DELETE SET NULL;

ALTER TABLE resellers
    ADD CONSTRAINT fk_resellers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

-- 6. INDEXATION
CREATE INDEX idx_resellers_status ON resellers (status);
CREATE INDEX idx_resellers_promo_code ON resellers (promo_code);
CREATE INDEX idx_withdrawals_reseller_status ON reseller_withdrawals (reseller_id, status);
CREATE INDEX idx_commissions_reseller_status ON reseller_commission_entries (reseller_id, status);
CREATE INDEX idx_commissions_booking ON reseller_commission_entries (booking_id);
CREATE INDEX idx_bookings_reseller ON bookings (reseller_id);
CREATE INDEX idx_users_reseller ON users (reseller_id);