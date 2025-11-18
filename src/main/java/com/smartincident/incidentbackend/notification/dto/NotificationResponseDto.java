package com.smartincident.incidentbackend.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

// DTO for real-time response
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {
    private String uid;
    private String title;
    private String message;
    private String type;
    private String relatedEntityUid;
    private String relatedEntityType;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private Boolean isRead;
    private List<String> channels;
}
