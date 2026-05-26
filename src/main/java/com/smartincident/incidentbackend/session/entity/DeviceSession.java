package com.smartincident.incidentbackend.session.entity;

import com.smartincident.incidentbackend.utils.Utils;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "device_sessions", indexes = {
        @Index(name = "idx_ds_user", columnList = "user_uid"),
        @Index(name = "idx_ds_device", columnList = "device_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(nullable = false, unique = true, length = 50)
    private String uid = Utils.generateUniqueID();

    @Column(name = "user_uid", nullable = false, length = 50)
    private String userUid;

    @Column(name = "user_name", length = 150)
    private String userName;

    @Column(name = "user_role", length = 30)
    private String userRole;

    /** FCM token or device UUID supplied by the client. */
    @Column(name = "device_id", length = 255)
    private String deviceId;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    /** ANDROID | IOS | WEB | UNKNOWN */
    @Column(length = 20)
    private String platform;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Builder.Default
    @Column(name = "login_at", nullable = false)
    private LocalDateTime loginAt = LocalDateTime.now();

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "logout_at")
    private LocalDateTime logoutAt;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
