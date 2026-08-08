package com.guentours.usernotification.web;

import com.guentours.usernotification.domain.UserNotification;

import java.time.Instant;

public record UserNotificationResponse(
        String id,
        String type,
        String title,
        String message,
        String relatedBookingId,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
    public static UserNotificationResponse from(UserNotification notification) {
        return new UserNotificationResponse(
                notification.getId(),
                notification.getType().name(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getRelatedBookingId(),
                notification.isRead(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
