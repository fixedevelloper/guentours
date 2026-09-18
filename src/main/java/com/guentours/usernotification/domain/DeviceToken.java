package com.guentours.usernotification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * An FCM registration token for one app install, used by {@code PushNotificationService} to
 * deliver push notifications. {@link #token} is unique across all users: the same physical
 * device/app-instance always overwrites its previous row on re-registration (token refresh, or a
 * different user logging in on a shared/reused device) rather than accumulating stale duplicates
 * that would otherwise keep receiving the old owner's pushes.
 */
@Entity
@Table(name = "device_tokens", indexes = {
        @Index(name = "idx_device_tokens_user_id", columnList = "user_id")
})
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    /** "ANDROID" / "IOS", client-reported and informational only - never used to branch send logic. */
    @Column(length = 20)
    private String platform;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected DeviceToken() {
        // JPA
    }

    public DeviceToken(String userId, String token, String platform) {
        this.userId = userId;
        this.token = token;
        this.platform = platform;
    }

    public void reassignTo(String userId) {
        this.userId = userId;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getToken() {
        return token;
    }

    public String getPlatform() {
        return platform;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
