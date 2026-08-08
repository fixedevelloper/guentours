package com.guentours.usernotification.web;

import com.guentours.security.SecurityUtils;
import com.guentours.usernotification.UserNotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
public class UserNotificationController {

    private final UserNotificationService notificationService;

    public UserNotificationController(UserNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Page<UserNotificationResponse> list(@PageableDefault(size = 20) Pageable pageable) {
        return notificationService.list(SecurityUtils.currentUserId(), pageable).map(UserNotificationResponse::from);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount() {
        return new UnreadCountResponse(notificationService.unreadCount(SecurityUtils.currentUserId()));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable String id) {
        notificationService.markRead(SecurityUtils.currentUserId(), id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        notificationService.markAllRead(SecurityUtils.currentUserId());
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return notificationService.subscribe(SecurityUtils.currentUserId());
    }

    public record UnreadCountResponse(long count) {
    }
}
