package com.guentours.storage.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "user_images", indexes = {
        @Index(name = "idx_user_images_owner_id", columnList = "owner_id")
})
public class UserImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "owner_id", nullable = false, length = 36)
    private String ownerId;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected UserImage() {
        // JPA
    }

    public UserImage(String ownerId, String url, String originalFilename, Long sizeBytes, String contentType) {
        this.ownerId = ownerId;
        this.url = url;
        this.originalFilename = originalFilename;
        this.sizeBytes = sizeBytes;
        this.contentType = contentType;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getUrl() { return url; }
    public String getOriginalFilename() { return originalFilename; }
    public Long getSizeBytes() { return sizeBytes; }
    public String getContentType() { return contentType; }
    public Instant getCreatedAt() { return createdAt; }
}