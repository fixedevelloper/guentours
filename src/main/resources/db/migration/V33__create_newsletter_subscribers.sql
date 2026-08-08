CREATE TABLE newsletter_subscribers (
    id                   VARCHAR(36)  NOT NULL,
    email                VARCHAR(255) NOT NULL,
    source               VARCHAR(50),
    unsubscribe_token    VARCHAR(36)  NOT NULL,
    subscribed_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_newsletter_subscribers_email UNIQUE (email),
    CONSTRAINT uq_newsletter_subscribers_unsubscribe_token UNIQUE (unsubscribe_token)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_newsletter_subscribers_email ON newsletter_subscribers (email);
