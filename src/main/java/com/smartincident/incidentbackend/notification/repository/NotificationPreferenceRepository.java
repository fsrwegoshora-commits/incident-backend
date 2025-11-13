package com.smartincident.incidentbackend.notification.repository;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.notification.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference,Long> {
    Optional<NotificationPreference> findByUser(User user);
}
