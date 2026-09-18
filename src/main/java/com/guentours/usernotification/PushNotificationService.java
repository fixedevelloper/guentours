package com.guentours.usernotification;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.guentours.usernotification.domain.DeviceToken;
import com.guentours.usernotification.domain.DeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Sends push notifications via Firebase Cloud Messaging to every device a user has registered
 * (see {@link DeviceToken}). Initializes the Firebase Admin SDK itself, best-effort, from {@link
 * PushNotificationProperties#credentialsPath()} - mirrors how {@code DigitwacePaymentGateway}
 * treats blank Digitwace keys: no real Firebase project configured yet just means push is a no-op,
 * never a startup failure.
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final DeviceTokenRepository deviceTokenRepository;
    private final FirebaseMessaging messaging;

    public PushNotificationService(DeviceTokenRepository deviceTokenRepository, PushNotificationProperties properties) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.messaging = initMessaging(properties.credentialsPath());
    }

    private static FirebaseMessaging initMessaging(String credentialsPath) {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("FIREBASE_CREDENTIALS_PATH is not set - push notifications are disabled.");
            return null;
        }
        try (InputStream credentials = new FileInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentials))
                    .build();
            FirebaseApp app = FirebaseApp.getApps().isEmpty()
                    ? FirebaseApp.initializeApp(options)
                    : FirebaseApp.getInstance();
            return FirebaseMessaging.getInstance(app);
        } catch (IOException e) {
            log.error("Failed to initialize the Firebase Admin SDK from '{}' - push notifications are disabled: {}",
                    credentialsPath, e.getMessage());
            return null;
        }
    }

    /**
     * Best-effort: never throws. A push failure (invalid/expired token, Firebase unreachable, not
     * configured at all) must never block the in-app notification it accompanies (see {@code
     * UserNotificationService#create}). A token Firebase reports as unregistered is deleted so it
     * stops being retried on every future notification.
     */
    public void sendToUser(String userId, String title, String body, Map<String, String> data) {
        if (messaging == null) {
            return;
        }
        List<DeviceToken> tokens = deviceTokenRepository.findByUserId(userId);
        for (DeviceToken deviceToken : tokens) {
            send(deviceToken, title, body, data);
        }
    }

    private void send(DeviceToken deviceToken, String title, String body, Map<String, String> data) {
        Message message = Message.builder()
                .setToken(deviceToken.getToken())
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .putAllData(data == null ? Map.of() : data)
                .build();
        try {
            messaging.send(message);
        } catch (FirebaseMessagingException e) {
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                log.info("Removing unregistered FCM token for user {}", deviceToken.getUserId());
                deviceTokenRepository.deleteByToken(deviceToken.getToken());
            } else {
                log.error("Failed to send push notification to user {}: {}", deviceToken.getUserId(), e.getMessage());
            }
        }
    }
}
