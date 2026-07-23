ALTER TABLE bookings ADD COLUMN partner_id VARCHAR(36) NULL;

-- Nécessaire si les réservations directes doivent couvrir les 2 nouvelles verticales
ALTER TABLE bookings MODIFY COLUMN offer_type
    ENUM('FLIGHT','HOTEL','CAR_RENTAL','FURNISHED_RENTAL') NOT NULL;

-- 'DIRECT' distingue une réservation sur inventaire partenaire d'une réservation GDS
ALTER TABLE bookings MODIFY COLUMN provider_type
    ENUM('SABRE','TRAVELOPRO','TRAVELPORT','DIRECT') NOT NULL;

CREATE INDEX idx_bookings_partner ON bookings (partner_id);