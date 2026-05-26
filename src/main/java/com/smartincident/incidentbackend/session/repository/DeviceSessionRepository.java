package com.smartincident.incidentbackend.session.repository;

import com.smartincident.incidentbackend.session.entity.DeviceSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceSessionRepository extends JpaRepository<DeviceSession, Long> {

    Optional<DeviceSession> findByDeviceIdAndUserUidAndActiveTrue(String deviceId, String userUid);

    List<DeviceSession> findByUserUidAndActiveTrue(String userUid);

    @Modifying
    @Transactional
    @Query("UPDATE DeviceSession d SET d.active = false, d.logoutAt = :now WHERE d.userUid = :userUid AND d.active = true")
    int logoutAllForUser(@Param("userUid") String userUid, @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("UPDATE DeviceSession d SET d.active = false, d.logoutAt = :now WHERE d.deviceId = :deviceId AND d.userUid = :userUid")
    int logoutDevice(@Param("deviceId") String deviceId, @Param("userUid") String userUid, @Param("now") LocalDateTime now);

    @Query("SELECT d FROM DeviceSession d " +
           "WHERE (:userUid IS NULL OR d.userUid = :userUid) " +
           "AND (:active IS NULL OR d.active = :active) " +
           "ORDER BY d.loginAt DESC")
    Page<DeviceSession> findFiltered(
            @Param("userUid") String userUid,
            @Param("active") Boolean active,
            Pageable pageable
    );
}
