CREATE TABLE vehicles (
    id VARCHAR(36) PRIMARY KEY,
    partner_id VARCHAR(36) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    year INT NOT NULL,
    category VARCHAR(20) NOT NULL,
    transmission VARCHAR(20) NOT NULL,
    seats INT NOT NULL,
    air_conditioning BOOLEAN NOT NULL,
    price_per_day DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    units_count INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE vehicle_pickup_locations (
    vehicle_id VARCHAR(36) NOT NULL,
    city VARCHAR(100) NOT NULL,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE
);

CREATE TABLE vehicle_availabilities (
    id VARCHAR(36) PRIMARY KEY,
    vehicle_id VARCHAR(36) NOT NULL,
    rent_date DATE NOT NULL,
    units_available INT NOT NULL,
    price_override DECIMAL(12,2),
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE,
    UNIQUE (vehicle_id, rent_date)
);

CREATE INDEX idx_vehicles_partner ON vehicles (partner_id);
CREATE INDEX idx_vehicle_availabilities_date ON vehicle_availabilities (rent_date);
