CREATE TABLE airline_flights (
    id VARCHAR(36) PRIMARY KEY,
    partner_id VARCHAR(36) NOT NULL,
    flight_number VARCHAR(20) NOT NULL,
    aircraft_type VARCHAR(100) NOT NULL,
    origin_airport_code VARCHAR(3) NOT NULL,
    destination_airport_code VARCHAR(3) NOT NULL,
    departure_time TIME NOT NULL,
    arrival_time TIME NOT NULL,
    duration_minutes INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    UNIQUE (partner_id, flight_number)
);

CREATE TABLE airline_flight_operating_days (
    flight_id VARCHAR(36) NOT NULL,
    day_of_week INT NOT NULL,
    FOREIGN KEY (flight_id) REFERENCES airline_flights(id) ON DELETE CASCADE
);

CREATE TABLE flight_fares (
    id VARCHAR(36) PRIMARY KEY,
    flight_id VARCHAR(36) NOT NULL,
    cabin_class VARCHAR(20) NOT NULL,
    base_price DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    baggage_allowance_kg INT NOT NULL,
    total_seats INT NOT NULL,
    FOREIGN KEY (flight_id) REFERENCES airline_flights(id) ON DELETE CASCADE
);

CREATE TABLE flight_availabilities (
    id VARCHAR(36) PRIMARY KEY,
    flight_fare_id VARCHAR(36) NOT NULL,
    flight_date DATE NOT NULL,
    seats_available INT NOT NULL,
    price_override DECIMAL(12,2),
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    FOREIGN KEY (flight_fare_id) REFERENCES flight_fares(id) ON DELETE CASCADE,
    UNIQUE (flight_fare_id, flight_date)
);

CREATE INDEX idx_airline_flights_partner ON airline_flights (partner_id);
CREATE INDEX idx_flight_availabilities_date ON flight_availabilities (flight_date);
