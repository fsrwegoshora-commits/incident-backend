package com.smartincident.incidentbackend.incident.entity;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.enums.MessageType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "chat_messages")
public class ChatMessage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_report_id", nullable = false)
    private IncidentReport relatedIncident;

    @Column(nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType;

    @Column(nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();

    private Boolean isRead = false;

    @Column(name = "media_url")
    private String mediaUrl;

    @Column(name = "media_file_name")
    private String mediaFileName;

    @Column(name = "media_file_size")
    private Long mediaFileSize;

    @Column(name = "media_duration")
    private Integer mediaDuration;

    @Column(name = "media_thumbnail_url")
    private String mediaThumbnailUrl;

    @Override
    public String toString() {
        return "ChatMessage{" +
                "id=" + getId() +
                ", uid='" + getUid() + '\'' +
                ", message='" + message + '\'' +
                ", messageType=" + messageType +
                ", sentAt=" + sentAt +
                ", mediaUrl='" + mediaUrl + '\'' +
                // OMIT sender and relatedIncident
                '}';
    }
}