ALTER TABLE properties
    ADD COLUMN cover_image_url VARCHAR(1024);

CREATE TABLE property_images
(
    id            VARCHAR(36) PRIMARY KEY,
    property_id   VARCHAR(36)   NOT NULL,
    url           VARCHAR(1024) NOT NULL,
    caption       VARCHAR(255),
    display_order INT                    DEFAULT 0,
    is_primary    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP              DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE CASCADE
);

CREATE INDEX idx_property_images_property ON property_images (property_id);
