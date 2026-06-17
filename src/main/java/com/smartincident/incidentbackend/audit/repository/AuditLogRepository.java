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

    @Query(value = "SELECT * FROM audit_logs " +
           "WHERE (:actorUid IS NULL OR actor_uid = :actorUid) " +
           "AND (:action IS NULL OR action = :action) " +
           "AND (:entityType IS NULL OR entity_type = :entityType) " +
           "AND (:category IS NULL OR category = :category) " +
           "AND (CAST(:from AS TIMESTAMP) IS NULL OR timestamp >= :from) " +
           "AND (CAST(:to AS TIMESTAMP) IS NULL OR timestamp <= :to) " +
           "AND (:key IS NULL OR description ILIKE '%' || :key || '%' " +
           "     OR endpoint ILIKE '%' || :key || '%') " +
           "ORDER BY timestamp DESC",
           countQuery = "SELECT COUNT(*) FROM audit_logs " +
           "WHERE (:actorUid IS NULL OR actor_uid = :actorUid) " +
           "AND (:action IS NULL OR action = :action) " +
           "AND (:entityType IS NULL OR entity_type = :entityType) " +
           "AND (:category IS NULL OR category = :category) " +
           "AND (CAST(:from AS TIMESTAMP) IS NULL OR timestamp >= :from) " +
           "AND (CAST(:to AS TIMESTAMP) IS NULL OR timestamp <= :to) " +
           "AND (:key IS NULL OR description ILIKE '%' || :key || '%' " +
           "     OR endpoint ILIKE '%' || :key || '%')",
           nativeQuery = true)
    Page<AuditLog> findFiltered(
            @Param("actorUid") String actorUid,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("category") String category,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("key") String key,
            Pageable pageable
    );
}
