package com.smartincident.incidentbackend.notification.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PushNotificationService {

    @Value("${app.notification.push.enabled:true}")
    private boolean pushEnabled;

    private final FirebaseApp firebaseApp;

    public PushNotificationService(FirebaseApp firebaseApp) {
        this.firebaseApp = firebaseApp;
        log.warn("PushNotificationService created - FirebaseApp injected successfully!");
    }

    private FirebaseMessaging getFirebaseMessaging() {
        try {
            return FirebaseMessaging.getInstance(firebaseApp);
        } catch (Exception e) {
            log.error("Firebase not ready: {}", e.getMessage());
            return null;
        }
    }

    public boolean sendToDevices(List<String> deviceTokens, String title, String message, Map<String, String> data) {
        log.info("📱 Sending push to {} devices, Title: {}, PushEnabled: {}",
                deviceTokens.size(), title, pushEnabled);

        if (!pushEnabled) {
            log.info("Push disabled in config");
            return false;
        }

        FirebaseMessaging messaging = getFirebaseMessaging();
        if (messaging == null) {
            log.error("FirebaseMessaging haipatikani - labda Firebase haija-initialize");
            return false;
        }

        if (deviceTokens == null || deviceTokens.isEmpty()) {
            log.warn("📱 No device tokens provided");
            return false;
        }

        FirebaseMessaging firebaseMessaging = getFirebaseMessaging();
        if (firebaseMessaging == null) {
            log.error("❌ FirebaseMessaging instance is null");
            return false;
        }

        try {
            List<List<String>> batches = splitIntoBatches(deviceTokens, 500);
            int successfulSends = 0;

            for (List<String> batch : batches) {
                log.info("📱 Processing batch of {} devices", batch.size());

                MulticastMessage multicastMessage = buildMulticastMessage(batch, title, message, data);
                BatchResponse response = firebaseMessaging.sendEachForMulticast(multicastMessage);

                successfulSends += response.getSuccessCount();
                log.info("📱 Batch result: {}/{} successful", response.getSuccessCount(), batch.size());

                // Handle failures
                if (response.getFailureCount() > 0) {
                    List<SendResponse> responses = response.getResponses();
                    for (int i = 0; i < responses.size(); i++) {
                        if (!responses.get(i).isSuccessful()) {
                            handleFcmError(responses.get(i).getException(), batch.get(i));
                        }
                    }
                }
            }

            boolean success = successfulSends > 0;
            log.info("📱 Final push result: {}/{} devices - Success: {}",
                    successfulSends, deviceTokens.size(), success);
            return success;

        } catch (FirebaseMessagingException e) {
            log.error("❌ FirebaseMessagingException: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("❌ Unexpected error sending push: {}", e.getMessage(), e);
            return false;
        }
    }

    private MulticastMessage buildMulticastMessage(List<String> deviceTokens, String title,
                                                   String message, Map<String, String> data) {
        MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
                .addAllTokens(deviceTokens)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(message)
                        .build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder()
                                .setSound("default")
                                .setBadge(1)
                                .build())
                        .build());

        // Add data payload for Flutter
        if (data != null && !data.isEmpty()) {
            messageBuilder.putAllData(data);
        } else {
            messageBuilder.putData("click_action", "FLUTTER_NOTIFICATION_CLICK");
        }

        return messageBuilder.build();
    }

    private void handleFcmError(Exception e, String deviceToken) {
        if (e instanceof FirebaseMessagingException) {
            FirebaseMessagingException fcmException = (FirebaseMessagingException) e;
            String errorCode = fcmException.getErrorCode().toString();
            log.warn("📱 FCM error for device {}: {}", deviceToken, errorCode);

            switch (errorCode) {
                case "invalid-argument":
                    log.error("❌ Invalid device token: {}", deviceToken);
                    break;
                case "unregistered":
                case "registration-token-not-registered":
                    log.info("📱 Device token is no longer registered: {}", deviceToken);
                    break;
                case "quota-exceeded":
                    log.error("❌ FCM quota exceeded");
                    break;
                default:
                    log.warn("⚠️ Unhandled FCM error: {} for device: {}", errorCode, deviceToken);
            }
        } else {
            log.warn("⚠️ Non-FCM error for device {}: {}", deviceToken, e.getMessage());
        }
    }

    private <T> List<List<T>> splitIntoBatches(List<T> originalList, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < originalList.size(); i += batchSize) {
            batches.add(originalList.subList(i, Math.min(i + batchSize, originalList.size())));
        }
        return batches;
    }

}