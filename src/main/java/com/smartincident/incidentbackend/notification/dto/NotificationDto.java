package com.smartincident.incidentbackend.notification.dto;


import com.smartincident.incidentbackend.enums.NotificationChannel;
import com.smartincident.incidentbackend.enums.NotificationType;
import com.smartincident.incidentbackend.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDto {
    private String title;
    private String message;
    private NotificationType type;
    private List<NotificationChannel> channels;
    private List<String> targetUserUids;
    private Role targetRole;
    private String targetStationUid;
    private String relatedEntityUid;
    private String relatedEntityType;
    private Map<String, Object> data;
}