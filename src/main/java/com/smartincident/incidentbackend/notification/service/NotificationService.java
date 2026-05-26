package com.smartincident.incidentbackend.notification.service;

import org.springframework.beans.factory.annotation.Value;
import com.smartincident.incidentbackend.enums.NotificationChannel;
import com.smartincident.incidentbackend.enums.NotificationType;
import com.smartincident.incidentbackend.notification.dto.NotificationDto;
import com.smartincident.incidentbackend.notification.dto.NotificationResponseDto;
import com.smartincident.incidentbackend.notification.entity.Notification;
import com.smartincident.incidentbackend.notification.repository.NotificationPreferenceRepository;
import com.smartincident.incidentbackend.notification.repository.NotificationRepository;
import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;
    private final PushNotificationService pushNotificationService;
    private final DeviceTokenService deviceTokenService;

    // Add WebSocket or Server-Sent Events support
    private final SimpMessagingTemplate simpMessagingTemplate;

    @Value("${app.notification.push.enabled:true}")
    private boolean pushEnabled;

    @Transactional
    public ResponseList<Notification> sendNotification(NotificationDto dto) {
        log.info("Sending in-app notification: {}", dto.getTitle());

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

                // 🔥 CRITICAL FIX: Check if PUSH channel is requested AND push is enabled
                boolean shouldSendPush = pushEnabled &&
                        dto.getChannels() != null &&
                        dto.getChannels().contains(NotificationChannel.PUSH);

                log.info("📱 Push notification check - Enabled: {}, Channels: {}, ShouldSend: {}",
                        pushEnabled, dto.getChannels(), shouldSendPush);

                if (shouldSendPush) {
                    boolean pushSent = sendPushNotificationToUser(user, dto);
                    log.info("📱 Push notification result for user {}: {}", user.getPhoneNumber(), pushSent);

                    if (pushSent) {
                        channels.add(NotificationChannel.PUSH);
                    }
                }

                notification.setChannels(channels);
                notification.setSentSuccessfully(true);

                Notification saved = notificationRepository.save(notification);
                sentNotifications.add(saved);

                // Broadcast real-time notification
                broadcastNotificationToUser(userUid, saved);
            }

            log.info("Sent {} notifications (in-app + push)", sentNotifications.size());
            return new ResponseList<>(sentNotifications);

        } catch (Exception e) {
            log.error("❌ Failed to send notifications: {}", e.getMessage(), e);
            return ResponseList.error("Failed to send notifications: " + e.getMessage());
        }
    }

    private boolean sendPushNotificationToUser(User user, NotificationDto dto) {
        try {
            // Check if push is enabled globally
            if (!pushEnabled) {
                log.info("📱 Push notifications disabled globally");
                return false;
            }

            List<String> deviceTokens = deviceTokenService.getActiveTokensByUserUid(user.getUid());

            if (deviceTokens == null || deviceTokens.isEmpty()) {
                log.warn("📱 No device tokens found for user: {}", user.getPhoneNumber());
                return false;
            }

            log.info("📱 Attempting push to {} devices for user: {}", deviceTokens.size(), user.getPhoneNumber());

            String relatedEntityUid = dto.getRelatedEntityUid() != null ? dto.getRelatedEntityUid() : "";
            String relatedEntityType = dto.getRelatedEntityType() != null ? dto.getRelatedEntityType() : "";

            Map<String, String> data = Map.of(
                    "type", dto.getType().name(),
                    "relatedEntityUid", relatedEntityUid,
                    "relatedEntityType", relatedEntityType,
                    "click_action", "FLUTTER_NOTIFICATION_CLICK",
                    "screen", getScreenForNotificationType(dto.getType())
            );

            boolean sent = pushNotificationService.sendToDevices(deviceTokens, dto.getTitle(), dto.getMessage(), data);

            log.info("📱 Push notification result for user {}: {}", user.getPhoneNumber(), sent);
            return sent;

        } catch (Exception e) {
            log.error("❌ Error sending push notification to user {}: {}", user.getPhoneNumber(), e.getMessage());
            return false;
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

                // ===== BROADCAST REAL-TIME NOTIFICATION =====
                broadcastNotificationToUser(user.getUid(), saved);
            }

            log.info("Sent {} notifications to role {}", sentNotifications.size(), dto.getTargetRole());
            return new ResponseList<>(sentNotifications);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseList.error("Failed to send notifications: " + e.getMessage());
        }
    }

    /**
     * Same as sendNotificationByRoleAndStation but queries by policeStation
     * (used for STATION_ADMIN / POLICE_OFFICER roles).
     */
    @Transactional
    public ResponseList<Notification> sendNotificationByRoleAndPoliceStation(NotificationDto dto) {
        log.info("Sending notification to role: {} at police station: {}", dto.getTargetRole(), dto.getTargetStationUid());

        if (dto.getTargetRole() == null)
            return ResponseList.error("Target role is required");

        try {
            List<User> targetUsers = dto.getTargetStationUid() != null
                    ? userRepository.findByRoleAndPoliceStation(dto.getTargetRole(), dto.getTargetStationUid())
                    : userRepository.findByRole(dto.getTargetRole());

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
                if (pushEnabled && dto.getChannels() != null && dto.getChannels().contains(NotificationChannel.PUSH)) {
                    if (sendPushNotificationToUser(user, dto)) channels.add(NotificationChannel.PUSH);
                }
                notification.setChannels(channels);
                notification.setSentSuccessfully(true);

                Notification saved = notificationRepository.save(notification);
                sentNotifications.add(saved);
                broadcastNotificationToUser(user.getUid(), saved);
            }
            log.info("Sent {} notifications to police station {}", sentNotifications.size(), dto.getTargetStationUid());
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

        return new ResponsePage<>(notificationRepository.findByUserUid(userUid, pageableParam.getPageable(true)));
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

        String currentUserUid = LoggedUser.getUid();
        if (!notification.getUser().getUid().equals(currentUserUid)) {
            return Response.error("Access denied");
        }

        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());

        try {
            Notification updated = notificationRepository.save(notification);
            log.debug("Notification marked as read: {}", notificationUid);
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
        log.debug("Unread notifications count for user {}: {}", userUid, count);
        return Response.success(count);
    }

    public ResponseList<Notification> clearNotifications(String userUid) {

        if (userUid == null)
            return new ResponseList<>("Id is required");

        Optional<User> oUser = userRepository.findByUid(userUid);
        if (!oUser.isPresent())
            return new ResponseList<>("Invalid id provided");

        User user = oUser.get();
        if (!user.getIsActive())
            return new ResponseList<>("User already deleted");

        List<Notification> notifications = notificationRepository.getByUserUid(userUid);

        if (notifications.isEmpty())
            return new ResponseList<>("NO notification to clear");

        List<Notification> toDelete = new ArrayList<>();

        for (Notification n : notifications) {
            n.setRead(true);
            n.delete();
            toDelete.add(n);
        }

        try {
            notificationRepository.saveAll(toDelete);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseList<>("Some notifications were not deleted");
        }

        return new ResponseList<>(toDelete);
    }

    public ResponseList<Notification> markAllRead(String userUid) {

        if (userUid == null)
            return new ResponseList<>("Id is required");

        Optional<User> oUser = userRepository.findByUid(userUid);
        if (!oUser.isPresent())
            return new ResponseList<>("Invalid id provided");

        User user = oUser.get();
        if (!user.getIsActive())
            return new ResponseList<>("User already deleted");

        List<Notification> notifications = notificationRepository.getNotReadByUserUid(userUid);

        if (notifications.isEmpty())
            return new ResponseList<>("NO notification to mark read");

        List<Notification> toRead = new ArrayList<>();

        for (Notification n : notifications) {
            n.setRead(true);
            toRead.add(n);
        }

        try {
            notificationRepository.saveAll(toRead);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseList<>("Some notifications were not marked as read");
        }

        return new ResponseList<>(toRead);
    }

    private void broadcastNotificationToUser(String userUid, Notification notification) {
        try {
            NotificationResponseDto notifDto = convertToNotificationResponse(notification);

            // Send via WebSocket to user
            String destination = "/user/" + userUid + "/queue/notifications";
            simpMessagingTemplate.convertAndSendToUser(userUid, "/queue/notifications", notifDto);

            log.info("✅ Real-time notification sent to user: {}", userUid);
        } catch (Exception e) {
            log.warn("⚠️ Failed to broadcast real-time notification: {}", e.getMessage());
        }
    }


    private NotificationResponseDto convertToNotificationResponse(Notification notification) {
        return NotificationResponseDto.builder()
                .uid(notification.getUid())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType().name())
                .relatedEntityUid(notification.getRelatedEntityUid())
                .relatedEntityType(notification.getRelatedEntityType())
                .sentAt(notification.getSentAt())
                .readAt(notification.getReadAt())
                .isRead(notification.getRead())
                .channels(notification.getChannels().stream().map(Enum::name).collect(Collectors.toList()))
                .build();
    }
}

