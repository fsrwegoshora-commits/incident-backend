package com.smartincident.incidentbackend.notification.service;

import com.smartincident.incidentbackend.enums.NotificationChannel;
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
//
//
//    @Value("${app.notification.sms.enabled:false}")
//    private boolean smsEnabled;
//
//    @Value("${app.notification.push.enabled:true}")
//    private boolean pushEnabled;
//
//    @Value("${app.notification.email.enabled:false}")
//    private boolean emailEnabled;


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

                notification.setChannels(Collections.singletonList(NotificationChannel.IN_APP));
                notification.setSentSuccessfully(true);

                Notification saved = notificationRepository.save(notification);
                sentNotifications.add(saved);
            }

            log.info("✅ Sent {} in-app notifications", sentNotifications.size());
            return new ResponseList<>(sentNotifications);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseList.error("Failed to send notifications");
        }
    }

    @Transactional
    public ResponseList<Notification> sendNotificationByRoleAndStation(NotificationDto dto) {
        log.info("Sending in-app notification to role: {} at station: {}", dto.getTargetRole(), dto.getTargetStationUid());

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
                notification.setChannels(Collections.singletonList(NotificationChannel.IN_APP));
                notification.setSentSuccessfully(true);

                Notification saved = notificationRepository.save(notification);
                sentNotifications.add(saved);
            }

            log.info("Sent {} in-app notifications to role {}", sentNotifications.size(), dto.getTargetRole());
            return new ResponseList<>(sentNotifications);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseList.error("Failed to send notifications");
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
            return Response.success(updated);
        } catch (Exception e) {
            log.error("Failed to mark notification as read: {}", e.getMessage());
            return Response.error("Failed to update notification");
        }
    }

    public Response<Long> getUnreadCount() {
        String userUid = LoggedUser.getUid();
        if (userUid == null) {
            return Response.error("User not authenticated");
        }

        long count = notificationRepository.countByUserUidAndReadFalse(userUid);
        return Response.success(count);
    }

//    @Transactional
//    public ResponseList<Notification> sendNotificationByRoleAndStation(NotificationDto dto) {
//        log.info("Sending notification to role: {} at station: {}", dto.getTargetRole(), dto.getTargetStationUid());
//
//        if (dto.getTargetRole() == null) {
//            return ResponseList.error("Target role is required");
//        }
//
//        try {
//            List<User> targetUsers;
//
//            if (dto.getTargetStationUid() != null) {
//                // Users with specific role at specific station
//                targetUsers = userRepository.findByRoleAndStation(dto.getTargetRole(), dto.getTargetStationUid());
//            } else {
//                // All users with specific role
//                targetUsers = userRepository.findByRole(dto.getTargetRole());
//            }
//
//            List<Notification> sentNotifications = new ArrayList<>();
//
//            for (User user : targetUsers) {
//                Notification notification = createAndSendNotification(dto, user);
//                if (notification != null) {
//                    sentNotifications.add(notification);
//                }
//            }
//
//            log.info("Sent {} notifications to role {} at station {}",
//                    sentNotifications.size(), dto.getTargetRole(), dto.getTargetStationUid());
//            return new ResponseList<>(sentNotifications);
//
//        } catch (Exception e) {
//            log.error("Failed to send role-based notifications: {}", e.getMessage());
//            return ResponseList.error("Failed to send notifications: " + Utils.getExceptionMessage(e));
//        }
//    }
//
//
//    private Notification createAndSendNotification(NotificationDto dto, User user) {
//        try {
//            // Get user's notification preferences
//            NotificationPreference preference = getUserNotificationPreference(user);
//
//            // Create notification entity
//            Notification notification = new Notification();
//            notification.setUser(user);
//            notification.setTitle(dto.getTitle());
//            notification.setMessage(dto.getMessage());
//            notification.setType(dto.getType());
//            notification.setRelatedEntityUid(dto.getRelatedEntityUid());
//            notification.setRelatedEntityType(dto.getRelatedEntityType());
//            notification.setSentAt(LocalDateTime.now());
//            notification.setChannels(new ArrayList<>());
//
//            // Send through enabled channels based on user preference
//            List<NotificationChannel> usedChannels = new ArrayList<>();
//
////            // SMS
////            if (smsEnabled && preference.isSmsEnabled() &&
////                    dto.getChannels().contains(NotificationChannel.SMS)) {
////                if (sendSmsNotification(user, dto)) {
////                    usedChannels.add(NotificationChannel.SMS);
////                }
////            }
//
////            // Push Notification
////            if (pushEnabled && preference.isPushEnabled() &&
////                    dto.getChannels().contains(NotificationChannel.PUSH)) {
////                if (sendPushNotification(user, dto)) {
////                    usedChannels.add(NotificationChannel.PUSH);
////                }
////            }
//
////            // Email
////            if (emailEnabled && preference.isEmailEnabled() &&
////                    dto.getChannels().contains(NotificationChannel.EMAIL)) {
////                if (sendEmailNotification(user, dto)) {
////                    usedChannels.add(NotificationChannel.EMAIL);
////                }
////            }
//
//            // In-App (always save for in-app notifications)
//            if (dto.getChannels().contains(NotificationChannel.IN_APP)) {
//                usedChannels.add(NotificationChannel.IN_APP);
//            }
//
//            notification.setChannels(usedChannels);
//            notification.setSentSuccessfully(!usedChannels.isEmpty());
//
//            // Save notification record
//            Notification savedNotification = notificationRepository.save(notification);
//            log.debug("Notification saved for user {}: {}", user.getPhoneNumber(), savedNotification.getUid());
//
//            return savedNotification;
//
//        } catch (Exception e) {
//            log.error("Failed to send notification to user {}: {}", user.getPhoneNumber(), e.getMessage());
//            return null;
//        }
//    }


    private boolean sendSmsNotification(User user, NotificationDto dto) {
        try {
            String phoneNumber = user.getPhoneNumber();
            String message = formatSmsMessage(dto);

            boolean sent = smsService.sendSms(phoneNumber, message);
            if (sent) {
                log.info("SMS sent to {}: {}", phoneNumber, dto.getTitle());
            }
            return sent;

        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", user.getPhoneNumber(), e.getMessage());
            return false;
        }
    }


//    private boolean sendPushNotification(User user, NotificationDto dto) {
//        try {
//            // Get user's FCM tokens or device IDs
//            List<String> deviceTokens = getUserDeviceTokens(user);
//
//            if (deviceTokens.isEmpty()) {
//                log.debug("No device tokens found for user: {}", user.getPhoneNumber());
//                return false;
//            }
//
//            boolean sent = pushNotificationService.sendPushNotification(
//                    deviceTokens, dto.getTitle(), dto.getMessage(), dto.getData()
//            );
//
//            if (sent) {
//                log.info("Push notification sent to user: {}", user.getPhoneNumber());
//            }
//            return sent;
//
//        } catch (Exception e) {
//            log.error("Failed to send push notification to {}: {}", user.getPhoneNumber(), e.getMessage());
//            return false;
//        }
//    }
//
//    /**
//     * Send email notification
//     */
//    private boolean sendEmailNotification(User user, NotificationDto dto) {
//        try {
//            // You might want to add email field to User entity
//            String email = user.getEmail(); // Assuming you add this field
//            if (email == null) {
//                return false;
//            }
//
//            boolean sent = emailService.sendEmail(
//                    email, dto.getTitle(), dto.getMessage(), dto.getData()
//            );
//
//            if (sent) {
//                log.info("Email sent to {}: {}", email, dto.getTitle());
//            }
//            return sent;
//
//        } catch (Exception e) {
//            log.error("Failed to send email to user {}: {}", user.getPhoneNumber(), e.getMessage());
//            return false;
//        }
//    }


    private String formatSmsMessage(NotificationDto dto) {
        String message = dto.getMessage();
        // Truncate if too long for SMS
        if (message.length() > 150) {
            message = message.substring(0, 147) + "...";
        }
        return message;
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


//    private List<String> getUserDeviceTokens(User user) {
//        // Implement based on your device token storage
//        // This could be from a separate DeviceToken entity
//        return Collections.emptyList(); // Placeholder
//    }



//    /**
//     * Update notification preferences
//     */
//    @Transactional
//    public Response<NotificationPreference> updatePreferences(NotificationPreference preferenceDto) {
//        String userUid = LoggedUser.getUid();
//        if (userUid == null) {
//            return Response.error("User not authenticated");
//        }
//
//        Optional<User> userOpt = userRepository.findByUid(userUid);
//        if (userOpt.isEmpty()) {
//            return Response.error("User not found");
//        }
//
//        NotificationPreference preference = preferenceRepository.findByUser(userOpt.get())
//                .orElse(new NotificationPreference());
//
//        preference.setUser(userOpt.get());
//        preference.setSmsEnabled(preferenceDto.isSmsEnabled());
//        preference.setPushEnabled(preferenceDto.isPushEnabled());
//        preference.setEmailEnabled(preferenceDto.isEmailEnabled());
//
//        try {
//            NotificationPreference saved = preferenceRepository.save(preference);
//            return Response.success(saved);
//        } catch (Exception e) {
//            log.error("Failed to update notification preferences: {}", e.getMessage());
//            return Response.error("Failed to update preferences");
//        }
//    }
}