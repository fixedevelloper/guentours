package com.guentours.newsletter.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "newsletter_subscribers")
@Getter
public class NewsletterSubscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 50)
    private String source;

    @Column(name = "unsubscribe_token", nullable = false, length = 36)
    private String unsubscribeToken;

    @Column(name = "subscribed_at", nullable = false)
    private Instant subscribedAt = Instant.now();

    protected NewsletterSubscriber() {
        // JPA
    }

    public NewsletterSubscriber(String email, String source) {
        this.email = email;
        this.source = source;
        this.unsubscribeToken = UUID.randomUUID().toString();
    }
}
