package com.guentours.usernotification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;

@Entity
@Table(name = "user_notifications")
@Getter
public class UserNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "related_booking_id", length = 36)
    private String relatedBookingId;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected UserNotification() {
        // JPA
    }

    public UserNotification(String userId, NotificationType type, String title, String message,
                             String relatedBookingId) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.relatedBookingId = relatedBookingId;
    }

    public void markRead() {
        if (this.read) {
            return;
        }
        this.read = true;
        this.readAt = Instant.now();
    }
}
