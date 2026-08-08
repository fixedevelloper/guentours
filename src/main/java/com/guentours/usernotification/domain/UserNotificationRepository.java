package com.guentours.usernotification.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface UserNotificationRepository extends JpaRepository<UserNotification, String> {

    Page<UserNotification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    long countByUserIdAndReadFalse(String userId);

    List<UserNotification> findByUserIdAndReadFalse(String userId);

    @Modifying
    @Query("delete from UserNotification n where n.read = true and n.readAt < :cutoff")
    void deleteAllReadBefore(@Param("cutoff") Instant cutoff);
}
