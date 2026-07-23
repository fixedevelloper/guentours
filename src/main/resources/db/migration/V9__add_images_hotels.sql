-- Migration : Ajout des tables d'images pour hôtels et types de chambres

-- 1. Table des images d'hôtels
CREATE TABLE hotel_images
(
    id            VARCHAR(36) PRIMARY KEY,
    hotel_id      VARCHAR(36)   NOT NULL,
    url           VARCHAR(1024) NOT NULL,
    caption       VARCHAR(255),
    display_order INT                    DEFAULT 0,
    is_primary    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP              DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (hotel_id) REFERENCES hotels (id) ON DELETE CASCADE
);

-- 2. Table des images des types de chambres
CREATE TABLE room_type_images
(
    id            VARCHAR(36) PRIMARY KEY,
    room_type_id  VARCHAR(36)   NOT NULL,
    url           VARCHAR(1024) NOT NULL,
    caption       VARCHAR(255),
    display_order INT                    DEFAULT 0,
    is_primary    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP              DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (room_type_id) REFERENCES room_types (id) ON DELETE CASCADE
);

-- 3. Index d'optimisation pour accélérer la récupération des galeries
CREATE INDEX idx_hotel_images_hotel ON hotel_images (hotel_id);
CREATE INDEX idx_room_type_images_room_type ON room_type_images (room_type_id);

-- 4. (Optionnel mais recommandé) Image de couverture directe sur la table principale
-- Permet d'éviter une jointure lors des affichages en liste (ex: recherche d'hôtels)
ALTER TABLE hotels
    ADD COLUMN cover_image_url VARCHAR(1024);
ALTER TABLE room_types
    ADD COLUMN cover_image_url VARCHAR(1024);