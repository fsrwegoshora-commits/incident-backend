package com.smartincident.incidentbackend.notification.service;

import com.google.api.client.util.Value;
import com.smartincident.incidentbackend.enums.NotificationChannel;
import com.smartincident.incidentbackend.enums.NotificationType;
import com.smartincident.incidentbackend.notification.dto.NotificationDto;
import com.smartincident.incidentbackend.notification.entity.Notification;
import com.smartincident.incidentbackend.notification.entity.NotificationPreference;
import com.smartincident.incidentbackend.notification.repository.NotificationPreferenceRepository;
import com.smartincident.incidentbackend.notification.repository.NotificationRepository;
import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final SmsService smsService;
    private final PushNotificationService pushNotificationService;
    private final DeviceTokenService deviceTokenService;

    @Value("${app.notification.push.enabled:true}")
    private boolean pushEnabled;

    @Transactional
    public ResponseList<Notification> sendNotification(NotificationDto dto) {
        log.info("Sending in-app notification: {}", dto.getTitle());

        // Basic validation
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            return ResponseList.error("Notification title is required");
        }
        if (dto.getTargetUserUids() == null || dto.getTargetUserUids().isEmpty()) {
            return ResponseList.error("Target users are required");
        }

        List<Notification> sentNotifications = new ArrayList<>();

        try {
            for (String userUid : dto.getTargetUserUids()) {
                Optional<User> userOpt = userRepository.findByUid(userUid);
                if (userOpt.isEmpty()) continue;

                User user = userOpt.get();

                Notification notification = new Notification();
                notification.setUser(user);
                notification.setTitle(dto.getTitle());
                notification.setMessage(dto.getMessage());
                notification.setType(dto.getType());
                notification.setRelatedEntityUid(dto.getRelatedEntityUid());
                notification.setRelatedEntityType(dto.getRelatedEntityType());
                notification.setSentAt(LocalDateTime.now());

                List<NotificationChannel> channels = new ArrayList<>();
                channels.add(NotificationChannel.IN_APP);

                if (pushEnabled && dto.getChannels().contains(NotificationChannel.PUSH)) {
                    boolean pushSent = sendPushNotificationToUser(user, dto);
                    if (pushSent) {
                        channels.add(NotificationChannel.PUSH);
                        log.debug(" Push notification sent to user: {}", user.getPhoneNumber());
                    }
                }

                notification.setChannels(channels);
                notification.setSentSuccessfully(true);

                Notification saved = notificationRepository.save(notification);
                sentNotifications.add(saved);
            }

            log.info("Sent {} notifications (in-app + push)", sentNotifications.size());
            return new ResponseList<>(sentNotifications);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseList.error("Failed to send notifications: " + e.getMessage());
        }
    }

    @Transactional
    public ResponseList<Notification> sendNotificationByRoleAndStation(NotificationDto dto) {
        log.info("Sending notification to role: {} at station: {}", dto.getTargetRole(), dto.getTargetStationUid());

        if (dto.getTargetRole() == null) {
            return ResponseList.error("Target role is required");
        }

        try {
            List<User> targetUsers;

            if (dto.getTargetStationUid() != null) {
                targetUsers = userRepository.findByRoleAndStation(dto.getTargetRole(), dto.getTargetStationUid());
            } else {
                targetUsers = userRepository.findByRole(dto.getTargetRole());
            }

            List<Notification> sentNotifications = new ArrayList<>();

            for (User user : targetUsers) {
                Notification notification = new Notification();
                notification.setUser(user);
                notification.setTitle(dto.getTitle());
                notification.setMessage(dto.getMessage());
                notification.setType(dto.getType());
                notification.setRelatedEntityUid(dto.getRelatedEntityUid());
                notification.setRelatedEntityType(dto.getRelatedEntityType());
                notification.setSentAt(LocalDateTime.now());

                List<NotificationChannel> channels = new ArrayList<>();
                channels.add(NotificationChannel.IN_APP);

                if (pushEnabled && dto.getChannels().contains(NotificationChannel.PUSH)) {
                    boolean pushSent = sendPushNotificationToUser(user, dto);
                    if (pushSent) {
                        channels.add(NotificationChannel.PUSH);
                    }
                }

                notification.setChannels(channels);
                notification.setSentSuccessfully(true);

                Notification saved = notificationRepository.save(notification);
                sentNotifications.add(saved);
            }

            log.info(" Sent {} notifications to role {}", sentNotifications.size(), dto.getTargetRole());
            return new ResponseList<>(sentNotifications);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseList.error("Failed to send notifications: " + e.getMessage());
        }
    }

    public ResponsePage<Notification> getUserNotifications(PageableParam pageableParam) {
        String userUid = LoggedUser.getUid();
        if (userUid == null) {
            return new ResponsePage<>("User not authenticated");
        }

        return new ResponsePage<>(notificationRepository.findByUserUid(
                userUid, pageableParam.getPageable(true)
        ));
    }


    private boolean sendPushNotificationToUser(User user, NotificationDto dto) {
        try {
            // Get user's device tokens
            List<String> deviceTokens = deviceTokenService.getActiveTokensByUserUid(user.getUid());

            if (deviceTokens.isEmpty()) {
                log.debug("📱 No device tokens found for user: {}", user.getPhoneNumber());
                return false;
            }

            // Prepare data for Flutter - FIX NULL SAFETY
            String relatedEntityUid = dto.getRelatedEntityUid() != null ? dto.getRelatedEntityUid() : "";
            String relatedEntityType = dto.getRelatedEntityType() != null ? dto.getRelatedEntityType() : "";

            Map<String, String> data = Map.of(
                    "type", dto.getType().name(),
                    "relatedEntityUid", relatedEntityUid,
                    "relatedEntityType", relatedEntityType,
                    "click_action", "FLUTTER_NOTIFICATION_CLICK",
                    "screen", getScreenForNotificationType(dto.getType())
            );

            // Send push notification
            boolean sent = pushNotificationService.sendToDevices(deviceTokens, dto.getTitle(), dto.getMessage(), data);

            if (sent) {
                log.info("📱 Push notification sent to user: {} ({} devices)", user.getPhoneNumber(), deviceTokens.size());
            } else {
                log.warn("📱 Push notification failed for user: {}", user.getPhoneNumber());
            }

            return sent;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getScreenForNotificationType(NotificationType type) {
        switch (type) {
            case INCIDENT_REPORTED:
            case INCIDENT_ASSIGNED:
            case INCIDENT_RESOLVED:
                return "/incidentDetails";
            case CHAT_MESSAGE:
                return "/chat";
            case SHIFT_ASSIGNED:
            case SHIFT_REASSIGNED:
                return "/shifts";
            default:
                return "/notifications";
        }
    }

    @Transactional
    public Response<Notification> markAsRead(String notificationUid) {
        Optional<Notification> notificationOpt = notificationRepository.findByUid(notificationUid);
        if (notificationOpt.isEmpty()) {
            return Response.error("Notification not found");
        }

        Notification notification = notificationOpt.get();

        // Verify ownership
        String currentUserUid = LoggedUser.getUid();
        if (!notification.getUser().getUid().equals(currentUserUid)) {
            return Response.error("Access denied");
        }

        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());

        try {
            Notification updated = notificationRepository.save(notification);
            log.debug(" Notification marked as read: {}", notificationUid);
            return Response.success(updated);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("Failed to update notification");
        }
    }

    public Response<Long> getUnreadCount() {
        String userUid = LoggedUser.getUid();
        if (userUid == null) {
            return Response.error("User not authenticated");
        }

        long count = notificationRepository.countByUserUidAndReadFalse(userUid);
        log.debug("📊 Unread notifications count for user {}: {}", userUid, count);
        return Response.success(count);
    }

    private NotificationPreference getUserNotificationPreference(User user) {
        return preferenceRepository.findByUser(user)
                .orElseGet(() -> {
                    NotificationPreference defaultPref = new NotificationPreference();
                    defaultPref.setUser(user);
                    defaultPref.setSmsEnabled(true);
                    defaultPref.setPushEnabled(true);
                    defaultPref.setEmailEnabled(false);
                    return preferenceRepository.save(defaultPref);
                });
    }
}