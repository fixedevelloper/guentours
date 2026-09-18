package com.guentours.usernotification.web;

import com.guentours.security.SecurityUtils;
import com.guentours.usernotification.DeviceTokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Registers/unregisters the FCM token of the current app install, so {@code PushNotificationService}
 *  knows where to deliver push notifications for the logged-in user. */
@RestController
@RequestMapping("/api/notifications/device-tokens")
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    public DeviceTokenController(DeviceTokenService deviceTokenService) {
        this.deviceTokenService = deviceTokenService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@Valid @RequestBody DeviceTokenRequest request) {
        deviceTokenService.register(SecurityUtils.currentUserId(), request.token(), request.platform());
    }

    /** Called on logout so a shared/reused device stops receiving pushes meant for the account
     *  that just signed out. {@code token} is a query param, not a path variable - FCM tokens can
     *  contain characters (e.g. "/") that break path segment routing. */
    @DeleteMapping
    public ResponseEntity<Void> unregister(@RequestParam String token) {
        deviceTokenService.unregister(token);
        return ResponseEntity.noContent().build();
    }
}
