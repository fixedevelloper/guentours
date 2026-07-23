CREATE TABLE properties (
    id VARCHAR(36) PRIMARY KEY,
    partner_id VARCHAR(36) NOT NULL,
    title VARCHAR(255) NOT NULL,
    property_type VARCHAR(20) NOT NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    bedrooms INT NOT NULL,
    bathrooms INT NOT NULL,
    max_guests INT NOT NULL,
    price_per_night DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    min_stay_nights INT NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE property_amenities (
    property_id VARCHAR(36) NOT NULL,
    amenity VARCHAR(100) NOT NULL,
    FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE
);

CREATE TABLE property_availabilities (
    id VARCHAR(36) PRIMARY KEY,
    property_id VARCHAR(36) NOT NULL,
    stay_date DATE NOT NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    price_override DECIMAL(12,2),
    FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
    UNIQUE (property_id, stay_date)
);

CREATE INDEX idx_properties_partner ON properties (partner_id);
CREATE INDEX idx_property_availabilities_date ON property_availabilities (stay_date);
