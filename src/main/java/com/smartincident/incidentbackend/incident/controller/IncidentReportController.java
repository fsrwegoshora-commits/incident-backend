package com.smartincident.incidentbackend.incident.controller;

import com.smartincident.incidentbackend.incident.dto.IncidentReportDto;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.incident.service.IncidentReportService;
import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.enums.IncidentStatus;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
@Slf4j
public class IncidentReportController {

    private final IncidentReportService incidentService;

    @Authenticated
    @PostMapping
    public Response<IncidentReport> createIncident(@RequestBody IncidentReportDto dto) {
        log.info("Creating incident: {}", dto.getTitle());
        return incidentService.createIncident(dto);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT, Role.POLICE_OFFICER})
    @PutMapping
    public Response<IncidentReport> updateIncident(@RequestBody IncidentReportDto dto) {
        log.info(" Updating incident: {}", dto.getUid());
        return incidentService.updateIncident(dto);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @PutMapping("/{incidentUid}/assign-officer/{officerUid}")
    public Response<IncidentReport> assignOfficer(@PathVariable String incidentUid, @PathVariable String officerUid) {
        log.info(" Assigning officer to incident");
        return incidentService.assignOfficer(incidentUid, officerUid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT, Role.CITIZEN})
    @DeleteMapping("/{uid}")
    public Response<IncidentReport> deleteIncident(@PathVariable String uid) {
        log.info(" Deleting incident: {}", uid);
        return incidentService.deleteIncident(uid);
    }

    @Authenticated
    @GetMapping("/{uid}")
    public Response<IncidentReport> getIncident(@PathVariable String uid) {
        return incidentService.getIncident(uid);
    }

    @Authenticated
    @GetMapping("/my")
    public ResponsePage<IncidentReport> getMyIncidents(@ModelAttribute PageableParam pageableParam) {
        log.info(" Getting my incidents");
        return incidentService.getMyIncidents(pageableParam);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @GetMapping("/station")
    public ResponsePage<IncidentReport> getStationIncidents(
            @ModelAttribute PageableParam pageableParam,
            @RequestParam(required = false) IncidentStatus status) {
        log.info(" Getting station incidents");
        return incidentService.getStationIncidents(pageableParam, status);
    }

    @Authenticated
    @AuthorizedRole({Role.POLICE_OFFICER, Role.STATION_ADMIN, Role.ROOT})
    @GetMapping("/officer")
    public ResponsePage<IncidentReport> getOfficerIncidents(
            @ModelAttribute PageableParam pageableParam,
            @RequestParam(required = false) IncidentStatus status) {
        log.info(" Getting officer incidents");
        return incidentService.getOfficerIncidents(pageableParam, status);
    }

    @Authenticated
    @AuthorizedRole({Role.POLICE_OFFICER, Role.STATION_ADMIN, Role.ROOT})
    @GetMapping("/nearby")
    public Response<List<IncidentReport>> getNearbyIncidents(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam Double radiusKm,
            @RequestParam(required = false) IncidentStatus status){
        log.info(" Getting nearby incidents");
        return incidentService.getNearbyIncidents(latitude, longitude, radiusKm, status);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @GetMapping("/stats")
    public Response<IncidentReportService.IncidentStats> getIncidentStats(@RequestParam(required = false) String stationUid) {
        return incidentService.getStationStats(stationUid);
    }
}
