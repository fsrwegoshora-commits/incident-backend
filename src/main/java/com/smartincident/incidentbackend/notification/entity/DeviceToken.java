package com.smartincident.incidentbackend.notification.entity;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Entity
@Table(name = "device_tokens")
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String token;

    @Column(name = "device_type")
    private String deviceType = "FLUTTER";

    @Column(name = "app_version")
    private String appVersion;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "is_active")
    private Boolean isActive = true;
}