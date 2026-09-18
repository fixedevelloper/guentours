package com.guentours.usernotification;

import com.guentours.usernotification.domain.DeviceToken;
import com.guentours.usernotification.domain.DeviceTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceTokenService {

    private final DeviceTokenRepository repository;

    public DeviceTokenService(DeviceTokenRepository repository) {
        this.repository = repository;
    }

    /**
     * Upsert on the token itself: it always ends up owned by whichever user most recently
     * registered it. Covers both an FCM token refresh for the same user and a different user
     * logging in on a shared/reused device, without ever leaving a stale row that would keep
     * pushing to a device after its owner changed.
     */
    @Transactional
    public void register(String userId, String token, String platform) {
        DeviceToken deviceToken = repository.findByToken(token)
                .map(existing -> {
                    existing.reassignTo(userId);
                    return existing;
                })
                .orElseGet(() -> new DeviceToken(userId, token, platform));
        repository.save(deviceToken);
    }

    @Transactional
    public void unregister(String token) {
        repository.deleteByToken(token);
    }
}
