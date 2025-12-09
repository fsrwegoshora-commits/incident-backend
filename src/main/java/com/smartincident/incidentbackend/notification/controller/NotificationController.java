package com.smartincident.incidentbackend.notification.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.notification.dto.NotificationDto;
import com.smartincident.incidentbackend.notification.entity.Notification;
import com.smartincident.incidentbackend.notification.service.NotificationService;
import com.smartincident.incidentbackend.utils.PageableParam;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponseList;
import com.smartincident.incidentbackend.utils.ResponsePage;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@GraphQLApi
@Slf4j
public class NotificationController {
    private final NotificationService notificationService;

    @Authenticated
    @GraphQLMutation(name = "sendNotification", description = "Citizen reports a notification")
    public ResponseList<Notification> sendNotification(@GraphQLArgument(name = "dto") NotificationDto dto) {
        log.info("Creating notification");
        return notificationService.sendNotification(dto);
    }
    @Authenticated
    @GraphQLMutation(name = "sendNotificationByRoleAndStation", description = "Citizen reports a new incident")
    public ResponseList<Notification> sendNotificationByRoleAndStation(@GraphQLArgument(name = "dto") NotificationDto dto) {
        log.info("Creating notification: {}", dto.getTitle());
        return notificationService.sendNotificationByRoleAndStation(dto);
    }

    @Authenticated
    @GraphQLQuery(name = "getUserNotifications", description = "Get all notifications reported by current user")
    public ResponsePage<Notification> getUserNotifications(@GraphQLArgument(name = "pageableParam") PageableParam pageableParam) {
        log.info(" Getting my notifications");
        return notificationService.getUserNotifications(pageableParam);
    }

    @Authenticated
    @GraphQLMutation(name = "markAsRead", description = "Get notification by UID")
    public Response<Notification> markAsRead(@GraphQLArgument(name = "notificationUid") String notificationUid) {
        return notificationService.markAsRead(notificationUid);
    }

    @Authenticated
    @GraphQLQuery(name = "getUnreadCount", description = "Get total unread notifications for current user")
    public Response<Long> getUnreadCount() {
        return notificationService.getUnreadCount();
    }

    @Authenticated
    @GraphQLMutation(name = "clearNotifications", description = "Get notifications by UID")
    public ResponseList<Notification> clearNotifications(@GraphQLArgument(name = "userUid") String userUid) {
        return notificationService.clearNotifications(userUid);
    }

    @Authenticated
    @GraphQLMutation(name = "markAllRead", description = "Get notifications by user")
    public ResponseList<Notification> markAllRead(@GraphQLArgument(name = "userUid") String userUid) {
        return notificationService.markAllRead(userUid);
    }
}
