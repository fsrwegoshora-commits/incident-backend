// [file name]: PushNotificationService.java
package com.smartincident.incidentbackend.notification.service;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    @Value("${app.notification.push.enabled:true}")
    private boolean pushEnabled;

    /**
     * Send push notification to single device
     */
    public boolean sendToDevice(String deviceToken, String title, String message, Map<String, String> data) {
        if (!pushEnabled) {
            log.debug("Push notifications are disabled");
            return false;
        }

        try {
            Message fcmMessage = buildMessage(deviceToken, title, message, data);
            String response = FirebaseMessaging.getInstance().send(fcmMessage);

            log.info(" Push notification sent successfully: {}", response);
            return true;

        } catch (FirebaseMessagingException e) {
            log.error(" Failed to send push notification: {}", e.getMessage());
            handleFcmError(e, deviceToken);
            return false;
        }
    }

    /**
     * Send push notification to multiple devices
     */
    public boolean sendToDevices(List<String> deviceTokens, String title, String message, Map<String, String> data) {
        if (!pushEnabled) {
            log.debug("Push notifications are disabled");
            return false;
        }

        if (deviceTokens == null || deviceTokens.isEmpty()) {
            log.warn("No device tokens provided");
            return false;
        }

        try {
            // Split into batches of 500 (FCM limit)
            List<List<String>> batches = splitIntoBatches(deviceTokens, 500);
            int successfulSends = 0;

            for (List<String> batch : batches) {
                MulticastMessage multicastMessage = buildMulticastMessage(batch, title, message, data);
                BatchResponse response = FirebaseMessaging.getInstance().sendMulticast(multicastMessage);

                successfulSends += response.getSuccessCount();

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

            log.info("Sent push notifications to {}/{} devices", successfulSends, deviceTokens.size());
            return successfulSends > 0;

        } catch (FirebaseMessagingException e) {
            log.error(" Failed to send multicast push notification: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Build FCM message for single device
     */
    private Message buildMessage(String deviceToken, String title, String message, Map<String, String> data) {
        Message.Builder messageBuilder = Message.builder()
                .setToken(deviceToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(message)
                        .build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setIcon("notification_icon")
                                .setColor("#FF0000")
                                .setSound("default")
                                .setChannelId("high_importance_channel")
                                .build())
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder()
                                .setAlert(ApsAlert.builder()
                                        .setTitle(title)
                                        .setBody(message)
                                        .build())
                                .setSound("default")
                                .setBadge(1)
                                .build())
                        .build());

        // Add data payload for Flutter
        if (data != null && !data.isEmpty()) {
            messageBuilder.putAllData(data);
        } else {
            // Default data for Flutter
            messageBuilder.putData("click_action", "FLUTTER_NOTIFICATION_CLICK");
        }

        return messageBuilder.build();
    }

    /**
     * Build FCM message for multiple devices
     */
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

    /**
     * Handle FCM errors
     */
    private void handleFcmError(FirebaseMessagingException e, String deviceToken) {
        String errorCode = String.valueOf(e.getErrorCode());
        log.warn("FCM error for device {}: {}", deviceToken, errorCode);

        switch (errorCode) {
            case "invalid-argument":
                log.error("Invalid device token: {}", deviceToken);
                break;
            case "unregistered":
            case "registration-token-not-registered":
                log.info("Device token is no longer registered: {}", deviceToken);
                // Hapa unaweza kutoa token invalid kwenye database yako
                break;
            case "quota-exceeded":
                log.error("FCM quota exceeded");
                break;
            default:
                log.warn("Unhandled FCM error: {} for device: {}", errorCode, deviceToken);
        }
    }

    /**
     * Split list into batches
     */
    private <T> List<List<T>> splitIntoBatches(List<T> originalList, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < originalList.size(); i += batchSize) {
            batches.add(originalList.subList(i, Math.min(i + batchSize, originalList.size())));
        }
        return batches;
    }
}