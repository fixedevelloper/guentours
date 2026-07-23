CREATE TABLE hotels (
    id VARCHAR(36) PRIMARY KEY,
    partner_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    star_rating INT,
    description VARCHAR(2000),
    check_in_time TIME,
    check_out_time TIME,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
);

CREATE TABLE hotel_amenities (
    hotel_id VARCHAR(36) NOT NULL,
    amenity VARCHAR(100) NOT NULL,
    FOREIGN KEY (hotel_id) REFERENCES hotels(id) ON DELETE CASCADE
);

CREATE TABLE room_types (
    id VARCHAR(36) PRIMARY KEY,
    hotel_id VARCHAR(36) NOT NULL,
    name VARCHAR(150) NOT NULL,
    max_adults INT NOT NULL,
    max_children INT NOT NULL,
    bed_type VARCHAR(100),
    size_sqm DOUBLE,
    base_price DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    total_rooms INT NOT NULL,
    FOREIGN KEY (hotel_id) REFERENCES hotels(id) ON DELETE CASCADE
);

CREATE TABLE room_availabilities (
    id VARCHAR(36) PRIMARY KEY,
    room_type_id VARCHAR(36) NOT NULL,
    stay_date DATE NOT NULL,
    rooms_available INT NOT NULL,
    price_override DECIMAL(12,2),
    FOREIGN KEY (room_type_id) REFERENCES room_types(id) ON DELETE CASCADE,
    UNIQUE (room_type_id, stay_date)
);

CREATE INDEX idx_hotels_partner ON hotels (partner_id);
CREATE INDEX idx_room_availabilities_date ON room_availabilities (stay_date);
