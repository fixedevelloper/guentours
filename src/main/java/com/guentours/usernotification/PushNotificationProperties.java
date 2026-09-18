package com.guentours.usernotification;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Path to the Firebase service account JSON (console Firebase > Project settings > Service
 * accounts > Generate new private key) - see PushNotificationService. Left blank until a Firebase
 * project exists; push notifications are then just skipped rather than failing app startup.
 */
@ConfigurationProperties(prefix = "app.firebase")
public record PushNotificationProperties(String credentialsPath) {
}
