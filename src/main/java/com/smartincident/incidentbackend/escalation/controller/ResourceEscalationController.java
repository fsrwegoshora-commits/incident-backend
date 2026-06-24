package com.smartincident.incidentbackend.escalation.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.enums.ResourceEscalationStatus;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.escalation.dto.ResourceEscalationDto;
import com.smartincident.incidentbackend.escalation.service.ResourceEscalationService;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponseList;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/resource-escalations")
@RequiredArgsConstructor
public class ResourceEscalationController {

    private final ResourceEscalationService service;

    /** Dispatcher / Post Supervisor escalates when no resources are available. */
    @Authenticated
    @RequiresPermission(Permission.ESCALATE_RESOURCE)
    @PostMapping
    public Response<?> escalate(@RequestBody ResourceEscalationDto dto) {
        return service.escalate(dto);
    }

    /** Station Admin / Agency Admin / ROOT responds to a pending escalation. */
    @Authenticated
    @RequiresPermission(Permission.MANAGE_STATIONS)
    @PutMapping("/{uid}/respond")
    public Response<?> respond(
            @PathVariable String uid,
            @RequestBody Map<String, String> body) {
        String notes  = body.getOrDefault("notes", "");
        String status = body.getOrDefault("status", "APPROVED");
        return service.respond(uid, notes, ResourceEscalationStatus.valueOf(status));
    }

    /** Get all escalations for a specific incident. */
    @Authenticated
    @GetMapping("/incident/{incidentUid}")
    public ResponseList<?> getByIncident(@PathVariable String incidentUid) {
        return service.getByIncident(incidentUid);
    }

    /** Get all pending escalations — for Station Admin / Agency Admin. */
    @Authenticated
    @RequiresPermission(Permission.MANAGE_STATIONS)
    @GetMapping("/pending")
    public ResponseList<?> getPending() {
        return service.getPending();
    }
}
