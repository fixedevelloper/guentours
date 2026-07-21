CREATE TABLE partners (
                          id VARCHAR(36) PRIMARY KEY,
                          partner_type VARCHAR(30) NOT NULL,
                          company_name VARCHAR(255) NOT NULL,
                          registration_number VARCHAR(100) NOT NULL UNIQUE,
                          contact_name VARCHAR(255) NOT NULL,
                          email VARCHAR(255) NOT NULL UNIQUE,
                          phone VARCHAR(30) NOT NULL,
                          city VARCHAR(100) NOT NULL,
                          country VARCHAR(100) NOT NULL,
                          fleet_or_units_count INT,
                          description VARCHAR(2000),
                          status VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW',
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          reviewed_at TIMESTAMP NULL
);

CREATE INDEX idx_partners_status ON partners (status);
CREATE INDEX idx_partners_type ON partners (partner_type);