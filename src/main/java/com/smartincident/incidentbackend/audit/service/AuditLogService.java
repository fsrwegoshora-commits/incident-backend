package com.smartincident.incidentbackend.audit.service;

import com.smartincident.incidentbackend.audit.entity.AuditLog;
import com.smartincident.incidentbackend.audit.repository.AuditLogRepository;
import com.smartincident.incidentbackend.utils.ResponsePage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository repository;

    @Async
    public void log(String actorUid, String actorName, String actorRole,
                    String action, String entityType, String entityUid,
                    String endpoint, String httpMethod, String description,
                    String ipAddress, boolean success) {
        log(actorUid, actorName, actorRole, action, entityType, entityUid,
                endpoint, httpMethod, description, ipAddress, success, null, null, null);
    }

    @Async
    public void log(String actorUid, String actorName, String actorRole,
                    String action, String entityType, String entityUid,
                    String endpoint, String httpMethod, String description,
                    String ipAddress, boolean success,
                    String oldValue, String newValue, String category) {
        try {
            AuditLog entry = AuditLog.builder()
                    .actorUid(actorUid)
                    .actorName(actorName)
                    .actorRole(actorRole)
                    .action(action)
                    .entityType(entityType)
                    .entityUid(entityUid)
                    .endpoint(endpoint)
                    .httpMethod(httpMethod)
                    .description(description)
                    .ipAddress(ipAddress)
                    .success(success)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .category(category)
                    .timestamp(LocalDateTime.now())
                    .build();
            repository.save(entry);
        } catch (Exception e) {
            log.warn("Failed to persist audit log: {}", e.getMessage());
        }
    }

    public ResponsePage<AuditLog> getLogs(
            String actorUid, String action, String entityType,
            LocalDateTime from, LocalDateTime to, String key,
            int page, int size) {
        return getLogs(actorUid, action, entityType, null, from, to, key, page, size);
    }

    public ResponsePage<AuditLog> getLogs(
            String actorUid, String action, String entityType, String category,
            LocalDateTime from, LocalDateTime to, String key,
            int page, int size) {
        Page<AuditLog> result = repository.findFiltered(
                actorUid, action, entityType, category, from, to,
                (key == null || key.isBlank()) ? null : key,
                PageRequest.of(page, size));
        return new ResponsePage<>(result);
    }
}
