package com.smartincident.incidentbackend.audit.controller;

import com.smartincident.incidentbackend.audit.entity.AuditLog;
import com.smartincident.incidentbackend.audit.service.AuditLogService;
import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.utils.ResponsePage;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogService auditLogService;

    /**
     * GET /api/audit/logs
     * Query params: actorUid, action, entityType, from, to, key, page, size
     */
    @Authenticated
    @RequiresPermission(Permission.VIEW_AUDIT_LOGS)
    @GetMapping("/logs")
    public ResponsePage<AuditLog> getLogs(
            @RequestParam(required = false) String actorUid,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) String key,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return auditLogService.getLogs(actorUid, action, entityType, from, to, key, page, size);
    }
}
