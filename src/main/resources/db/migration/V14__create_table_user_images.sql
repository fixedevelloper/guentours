CREATE TABLE user_images (
                             id VARCHAR(36) PRIMARY KEY,
                             owner_id VARCHAR(36) NOT NULL,
                             url VARCHAR(2048) NOT NULL,
                             original_filename VARCHAR(255),
                             size_bytes BIGINT,
                             content_type VARCHAR(100),
                             created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_user_images_owner_id ON user_images(owner_id);