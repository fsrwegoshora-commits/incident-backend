package com.smartincident.incidentbackend.audit.repository;

import com.smartincident.incidentbackend.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("SELECT a FROM AuditLog a " +
           "WHERE (:actorUid IS NULL OR a.actorUid = :actorUid) " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND (:entityType IS NULL OR a.entityType = :entityType) " +
           "AND (:from IS NULL OR a.timestamp >= :from) " +
           "AND (:to IS NULL OR a.timestamp <= :to) " +
           "AND (:key IS NULL OR LOWER(a.description) LIKE LOWER(CONCAT('%', :key, '%')) " +
           "     OR LOWER(a.endpoint) LIKE LOWER(CONCAT('%', :key, '%'))) " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> findFiltered(
            @Param("actorUid") String actorUid,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("key") String key,
            Pageable pageable
    );
}
