
package com.smartincident.incidentbackend.notification.entity;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Entity
@Table(name = "notification_preferences")
@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationPreference extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "sms_enabled")
    private boolean smsEnabled = true;

    @Column(name = "push_enabled")
    private boolean pushEnabled = true;

    @Column(name = "email_enabled")
    private boolean emailEnabled = false;
}