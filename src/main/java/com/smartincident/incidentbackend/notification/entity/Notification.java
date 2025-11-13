// [file name]: Notification.java
package com.smartincident.incidentbackend.notification.entity;

import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.enums.NotificationChannel;
import com.smartincident.incidentbackend.enums.NotificationType;
import com.smartincident.incidentbackend.authotp.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "notifications")
@Data
@EqualsAndHashCode(callSuper = true)
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @ElementCollection
    @Enumerated(EnumType.STRING)
    private List<NotificationChannel> channels;

    private String relatedEntityUid;
    private String relatedEntityType;

    private LocalDateTime sentAt;
    private Boolean read = false;
    private LocalDateTime readAt;
    private Boolean sentSuccessfully = true;
}