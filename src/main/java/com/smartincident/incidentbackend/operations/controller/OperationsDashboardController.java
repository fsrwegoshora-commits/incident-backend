package com.smartincident.incidentbackend.operations.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.operations.dto.NationalDashboardDto;
import com.smartincident.incidentbackend.operations.dto.OperationsDashboardDto;
import com.smartincident.incidentbackend.operations.service.NationalDashboardService;
import com.smartincident.incidentbackend.operations.service.OperationsDashboardService;
import com.smartincident.incidentbackend.utils.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/operations")
@RequiredArgsConstructor
@Slf4j
public class OperationsDashboardController {

    private final OperationsDashboardService service;
    private final NationalDashboardService nationalDashboardService;

    /**
     * Live command dashboard — active incidents, unit statuses, on-duty counts.
     * Accessible to supervisors, admins, and ROOT. AGENCY_ADMIN sees only their agency.
     */
    @Authenticated
    @AuthorizedRole({Role.DISPATCHER_SUPERVISOR, Role.DISPATCH_CENTER_ADMIN,
                     Role.STATION_ADMIN, Role.AGENCY_ADMIN, Role.ROOT})
    @GetMapping("/dashboard")
    public Response<OperationsDashboardDto> getDashboard() {
        log.info("Loading operations dashboard");
        return service.getDashboard();
    }

    /**
     * National Emergency Coordination Dashboard — nationwide view.
     * All agencies, all resources, regional breakdown.
     * ROOT only (national command centre use).
     */
    @Authenticated
    @AuthorizedRole({Role.ROOT, Role.AGENCY_ADMIN})
    @GetMapping("/national")
    public Response<NationalDashboardDto> getNationalDashboard() {
        log.info("Loading national command dashboard");
        return nationalDashboardService.getNationalDashboard();
    }
}
