package com.smartincident.incidentbackend.notification.repository;

import com.smartincident.incidentbackend.notification.entity.Notification;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification,Long> {
    long countByUserUidAndReadFalse(String userUid);

    Optional<Notification> findByUid(String notificationUid);

    @Query("select n from Notification n where n.user.uid = :userUid and n.isActive=true")
    Page<Notification> findByUserUid(@Param("userUid") String userUid, Pageable pageable);

    @Query("select n from Notification n where n.user.uid = :userUid and n.isActive=true")
    List<Notification> getByUserUid(String userUid);

    @Query("select n from Notification n where n.user.uid = :userUid and n.read = false and n.isActive=true and n.type = com.smartincident.incidentbackend.enums.NotificationType.CHAT_MESSAGE")
    List<Notification> getNotReadByUserUid(String userUid);
}
