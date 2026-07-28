ALTER TABLE vehicles
    ADD COLUMN cover_image_url VARCHAR(1024);

CREATE TABLE vehicle_images
(
    id            VARCHAR(36) PRIMARY KEY,
    vehicle_id    VARCHAR(36)   NOT NULL,
    url           VARCHAR(1024) NOT NULL,
    caption       VARCHAR(255),
    display_order INT                    DEFAULT 0,
    is_primary    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP              DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles (id) ON DELETE CASCADE
);

CREATE INDEX idx_vehicle_images_vehicle ON vehicle_images (vehicle_id);
