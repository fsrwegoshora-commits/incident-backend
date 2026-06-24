package com.smartincident.incidentbackend.incident.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.incident.dto.AfterActionReportDto;
import com.smartincident.incidentbackend.incident.entity.AfterActionReport;
import com.smartincident.incidentbackend.incident.service.AfterActionReportService;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/after-action-reports")
@RequiredArgsConstructor
@Slf4j
public class AfterActionReportController {

    private final AfterActionReportService aarService;

    /** Create or update an after-action report. Only for RESOLVED/CLOSED incidents. */
    @Authenticated
    @RequiresPermission(Permission.SUBMIT_AFTER_ACTION_REPORT)
    @PostMapping
    public Response<AfterActionReport> createOrUpdate(@RequestBody AfterActionReportDto dto) {
        log.info("Saving after-action report for incident: {}", dto.getIncidentUid());
        return aarService.createOrUpdate(dto);
    }

    /** Supervisor/Admin approves the report. Locks it from further edits. */
    @Authenticated
    @RequiresPermission(Permission.MANAGE_AFTER_ACTION_REPORT)
    @PostMapping("/{uid}/approve")
    public Response<AfterActionReport> approve(@PathVariable String uid) {
        log.info("Approving after-action report: {}", uid);
        return aarService.approve(uid);
    }

    /** Get report by UID. */
    @Authenticated
    @RequiresPermission(Permission.VIEW_AFTER_ACTION_REPORT)
    @GetMapping("/{uid}")
    public Response<AfterActionReport> getByUid(@PathVariable String uid) {
        return aarService.getByUid(uid);
    }

    /** Get report for a specific incident. */
    @Authenticated
    @RequiresPermission(Permission.VIEW_AFTER_ACTION_REPORT)
    @GetMapping("/incident/{incidentUid}")
    public Response<AfterActionReport> getByIncident(@PathVariable String incidentUid) {
        return aarService.getByIncident(incidentUid);
    }

    /** Paginated list — filter by approved status and/or author. */
    @Authenticated
    @RequiresPermission(Permission.MANAGE_AFTER_ACTION_REPORT)
    @GetMapping
    public ResponsePage<AfterActionReport> getAll(
            @ModelAttribute PageableParam pageableParam,
            @RequestParam(required = false) Boolean approved,
            @RequestParam(required = false) String authorUid) {
        return aarService.getAll(pageableParam, approved, authorUid);
    }
}
