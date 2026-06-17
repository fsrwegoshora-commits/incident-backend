package com.smartincident.incidentbackend.incident.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.incident.dto.MultiAgencyDashboardDto;
import com.smartincident.incidentbackend.incident.service.MultiAgencyDashboardService;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponseList;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/multi-agency")
@RequiredArgsConstructor
public class MultiAgencyDashboardController {

    private final MultiAgencyDashboardService dashboardService;

    /** Summary metrics across all agencies and active incidents. */
    @Authenticated
    @AuthorizedRole({Role.DISPATCHER_SUPERVISOR, Role.DISPATCH_CENTER_ADMIN, Role.AGENCY_ADMIN, Role.ROOT})
    @GetMapping("/summary")
    public Response<MultiAgencyDashboardDto.DashboardSummary> getSummary() {
        return dashboardService.getDashboardSummary();
    }

    /** List of all active incidents requiring multi-agency coordination. */
    @Authenticated
    @AuthorizedRole({Role.DISPATCHER, Role.DISPATCHER_SUPERVISOR, Role.DISPATCH_CENTER_ADMIN,
                     Role.AGENCY_ADMIN, Role.ROOT})
    @GetMapping("/incidents")
    public ResponseList<MultiAgencyDashboardDto> getActiveMultiAgencyIncidents() {
        return dashboardService.getActiveMultiAgencyIncidents();
    }

    /** Coordination detail for a single incident — all agency statuses. */
    @Authenticated
    @AuthorizedRole({Role.DISPATCHER, Role.DISPATCHER_SUPERVISOR, Role.DISPATCH_CENTER_ADMIN,
                     Role.AGENCY_ADMIN, Role.ROOT, Role.STATION_ADMIN})
    @GetMapping("/incidents/{incidentUid}")
    public Response<MultiAgencyDashboardDto> getIncidentCoordination(@PathVariable String incidentUid) {
        return dashboardService.getIncidentCoordination(incidentUid);
    }
}
