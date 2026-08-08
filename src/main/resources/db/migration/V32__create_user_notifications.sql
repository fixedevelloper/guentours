CREATE TABLE user_notifications (
    id                  VARCHAR(36)   NOT NULL,
    user_id             VARCHAR(36)   NOT NULL,
    type                VARCHAR(30)   NOT NULL,
    title               VARCHAR(255)  NOT NULL,
    message             VARCHAR(1000) NOT NULL,
    related_booking_id  VARCHAR(36),
    is_read             BOOLEAN       NOT NULL DEFAULT FALSE,
    read_at             TIMESTAMP     NULL,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_user_notifications_user_read_created ON user_notifications (user_id, is_read, created_at);
CREATE INDEX idx_user_notifications_read_read_at ON user_notifications (is_read, read_at);
