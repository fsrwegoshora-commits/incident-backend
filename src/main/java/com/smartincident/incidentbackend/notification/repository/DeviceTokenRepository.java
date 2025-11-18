package com.smartincident.incidentbackend.notification.repository;

import com.smartincident.incidentbackend.notification.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findByUserUidAndIsActiveTrue(String userUid);

    @Query("SELECT dt.token FROM DeviceToken dt WHERE dt.user.uid = :userUid AND dt.isActive = true")
    List<String> findActiveTokensByUserUid(@Param("userUid") String userUid);

    @Modifying
    @Query("UPDATE DeviceToken dt SET dt.isActive = false WHERE dt.token = :token")
    void deactivateByToken(@Param("token") String token);

}