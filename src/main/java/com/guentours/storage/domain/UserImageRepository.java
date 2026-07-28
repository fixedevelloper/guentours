package com.guentours.storage.domain;

import com.guentours.storage.domain.UserImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserImageRepository extends JpaRepository<UserImage, String> {
    List<UserImage> findByOwnerIdOrderByCreatedAtDesc(String ownerId);
    Optional<UserImage> findByIdAndOwnerId(String id, String ownerId);
}
