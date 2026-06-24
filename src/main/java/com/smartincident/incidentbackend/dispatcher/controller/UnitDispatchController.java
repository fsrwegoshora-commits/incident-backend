package com.smartincident.incidentbackend.dispatcher.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.dispatcher.dto.UnitDispatchRequest;
import com.smartincident.incidentbackend.dispatcher.service.UnitDispatchService;
import com.smartincident.incidentbackend.enums.Permission;
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
    @RequiresPermission(Permission.DISPATCH_UNITS)
    @PostMapping("/{incidentUid}/dispatch-units")
    public Response<IncidentReport> dispatchUnits(
            @PathVariable String incidentUid,
            @RequestBody UnitDispatchRequest request) {
        log.info("Unit dispatch requested for incident: {}", incidentUid);
        return unitDispatchService.dispatchUnits(incidentUid, request);
    }

    /**
     * Operational monitor: all currently active (DISPATCHED / IN_PROGRESS) incidents.
     * Used by DISPATCH_CENTER_ADMIN / DISPATCHER_SUPERVISOR for oversight, and by
     * STATION_ADMIN for the Post Operations dashboard (which has VIEW_PENDING_INCIDENTS
     * but not VIEW_SLA_METRICS).
     */
    @Authenticated
    @RequiresPermission(value = {Permission.VIEW_SLA_METRICS, Permission.VIEW_PENDING_INCIDENTS}, requireAll = false)
    @GetMapping("/operational")
    public Response<List<IncidentReport>> getOperationalIncidents() {
        return unitDispatchService.getOperationalIncidents();
    }
}
