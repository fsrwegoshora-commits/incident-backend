package com.smartincident.incidentbackend.notification.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.notification.dto.NotificationDto;
import com.smartincident.incidentbackend.notification.entity.Notification;
import com.smartincident.incidentbackend.notification.service.NotificationService;
import com.smartincident.incidentbackend.utils.PageableParam;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponseList;
import com.smartincident.incidentbackend.utils.ResponsePage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {
    private final NotificationService notificationService;

    @Authenticated
    @PostMapping("/send")
    public ResponseList<Notification> sendNotification(@RequestBody NotificationDto dto) {
        log.info("Creating notification");
        return notificationService.sendNotification(dto);
    }

    @Authenticated
    @PostMapping("/send-by-role-station")
    public ResponseList<Notification> sendNotificationByRoleAndStation(@RequestBody NotificationDto dto) {
        log.info("Creating notification: {}", dto.getTitle());
        return notificationService.sendNotificationByRoleAndStation(dto);
    }

    @Authenticated
    @GetMapping
    public ResponsePage<Notification> getUserNotifications(@ModelAttribute PageableParam pageableParam) {
        log.info(" Getting my notifications");
        return notificationService.getUserNotifications(pageableParam);
    }

    @Authenticated
    @PutMapping("/{notificationUid}/read")
    public Response<Notification> markAsRead(@PathVariable String notificationUid) {
        return notificationService.markAsRead(notificationUid);
    }

    @Authenticated
    @GetMapping("/unread-count")
    public Response<Long> getUnreadCount() {
        return notificationService.getUnreadCount();
    }

    @Authenticated
    @DeleteMapping("/clear")
    public ResponseList<Notification> clearNotifications() {
        return notificationService.clearNotifications();
    }

    @Authenticated
    @PutMapping("/mark-all-read")
    public ResponseList<Notification> markAllRead() {
        return notificationService.markAllRead();
    }
}
