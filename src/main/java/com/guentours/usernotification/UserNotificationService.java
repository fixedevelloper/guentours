package com.guentours.usernotification;

import com.guentours.shared.exception.NotFoundException;
import com.guentours.usernotification.domain.NotificationType;
import com.guentours.usernotification.domain.UserNotification;
import com.guentours.usernotification.domain.UserNotificationRepository;
import com.guentours.usernotification.web.UserNotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;

@Service
public class UserNotificationService {

    private final UserNotificationRepository repository;
    private final UserNotificationSseRegistry sseRegistry;

    public UserNotificationService(UserNotificationRepository repository, UserNotificationSseRegistry sseRegistry) {
        this.repository = repository;
        this.sseRegistry = sseRegistry;
    }

    @Transactional
    public void create(String userId, NotificationType type, String title, String message, String relatedBookingId) {
        UserNotification notification = new UserNotification(userId, type, title, message, relatedBookingId);
        repository.save(notification);
        sseRegistry.publish(userId, UserNotificationResponse.from(notification));
    }

    public Page<UserNotification> list(String userId, Pageable pageable) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public long unreadCount(String userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(String userId, String notificationId) {
        UserNotification notification = repository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification " + notificationId + " not found"));
        if (!notification.getUserId().equals(userId)) {
            throw new AccessDeniedException("This notification does not belong to the current user");
        }
        notification.markRead();
        repository.save(notification);
    }

    @Transactional
    public void markAllRead(String userId) {
        List<UserNotification> unread = repository.findByUserIdAndReadFalse(userId);
        unread.forEach(UserNotification::markRead);
        repository.saveAll(unread);
    }

    public SseEmitter subscribe(String userId) {
        return sseRegistry.subscribe(userId);
    }

    @Transactional
    public void cleanupReadOlderThan(Instant cutoff) {
        repository.deleteAllReadBefore(cutoff);
    }
}
