package com.smartincident.incidentbackend.dispatcher.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.dispatcher.dto.UnitDispatchRequest;
import com.smartincident.incidentbackend.dispatcher.service.UnitDispatchService;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.utils.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
@Slf4j
public class UnitDispatchController {

    private final UnitDispatchService unitDispatchService;

    /**
     * Unit-based dispatch: assign police station, fire unit, and/or medical unit to an incident.
     * The system automatically assigns on-duty crew from each unit.
     */
    @Authenticated
    @AuthorizedRole({Role.ROOT, Role.DISPATCH_CENTER_ADMIN, Role.DISPATCHER_SUPERVISOR, Role.DISPATCHER})
    @PostMapping("/{incidentUid}/dispatch-units")
    public Response<IncidentReport> dispatchUnits(
            @PathVariable String incidentUid,
            @RequestBody UnitDispatchRequest request) {
        log.info("Unit dispatch requested for incident: {}", incidentUid);
        return unitDispatchService.dispatchUnits(incidentUid, request);
    }

    /**
     * Operational monitor: all currently active (DISPATCHED / IN_PROGRESS) incidents.
     * Used by DISPATCH_CENTER_ADMIN and DISPATCHER_SUPERVISOR for oversight.
     */
    @Authenticated
    @AuthorizedRole({Role.ROOT, Role.AGENCY_ADMIN, Role.DISPATCH_CENTER_ADMIN, Role.DISPATCHER_SUPERVISOR, Role.DISPATCHER})
    @GetMapping("/operational")
    public Response<List<IncidentReport>> getOperationalIncidents() {
        return unitDispatchService.getOperationalIncidents();
    }
}
