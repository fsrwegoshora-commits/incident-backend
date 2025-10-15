package com.smartincident.incidentbackend.incident.controller;

import com.smartincident.incidentbackend.incident.dto.IncidentReportDto;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.incident.service.IncidentReportService;
import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.enums.IncidentStatus;
import com.smartincident.incidentbackend.utils.*;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
@GraphQLApi
@RequiredArgsConstructor
@Slf4j
public class IncidentReportController {

    private final IncidentReportService incidentService;

    @Authenticated
    @AuthorizedRole({Role.CITIZEN,Role.STATION_ADMIN,Role.ROOT})
    @GraphQLMutation(name = "createIncident", description = "Citizen reports a new incident")
    public Response<IncidentReport> createIncident(@GraphQLArgument(name = "incidentDto") IncidentReportDto dto) {
        log.info("Creating incident: {}", dto.getTitle());
        return incidentService.createIncident(dto);
    }


    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT, Role.POLICE_OFFICER})
    @GraphQLMutation(name = "updateIncident", description = "Update incident details or status")
    public Response<IncidentReport> updateIncident(@GraphQLArgument(name = "incidentDto") IncidentReportDto dto) {
        log.info("✏ Updating incident: {}", dto.getUid());
        return incidentService.updateIncident(dto);
    }


    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @GraphQLMutation(name = "assignOfficerToIncident", description = "Assign officer to handle incident")
    public Response<IncidentReport> assignOfficer(@GraphQLArgument(name = "incidentUid") String incidentUid, @GraphQLArgument(name = "officerUid") String officerUid) {
        log.info("👮 Assigning officer to incident");
        return incidentService.assignOfficer(incidentUid, officerUid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT, Role.CITIZEN})
    @GraphQLMutation(name = "deleteIncident", description = "Delete an incident")
    public Response<IncidentReport> deleteIncident(@GraphQLArgument(name = "uid") String uid) {
        log.info(" Deleting incident: {}", uid);
        return incidentService.deleteIncident(uid);
    }

    @Authenticated
    @GraphQLQuery(name = "getIncident", description = "Get incident by UID")
    public Response<IncidentReport> getIncident(@GraphQLArgument(name = "uid") String uid) {
        return incidentService.getIncident(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.CITIZEN,Role.POLICE_OFFICER,Role.STATION_ADMIN,Role.ROOT})
    @GraphQLQuery(name = "getMyIncidents", description = "Get all incidents reported by current user")
    public ResponsePage<IncidentReport> getMyIncidents(
            @GraphQLArgument(name = "pageableParam") PageableParam pageableParam
    ) {
        log.info(" Getting my incidents");
        return incidentService.getMyIncidents(pageableParam);
    }


    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @GraphQLQuery(name = "getStationIncidents", description = "Get all incidents for a police station")
    public ResponsePage<IncidentReport> getStationIncidents(
            @GraphQLArgument(name = "pageableParam") PageableParam pageableParam,
            @GraphQLArgument(name = "status") IncidentStatus status
    ) {
        log.info("🏢 Getting station incidents");
        return incidentService.getStationIncidents(pageableParam, status);
    }

    @Authenticated
    @AuthorizedRole({Role.POLICE_OFFICER,Role.STATION_ADMIN,Role.ROOT})
    @GraphQLQuery(name = "getOfficerIncidents", description = "Get incidents assigned to current officer")
    public ResponsePage<IncidentReport> getOfficerIncidents(
            @GraphQLArgument(name = "pageableParam") PageableParam pageableParam,
            @GraphQLArgument(name = "status") IncidentStatus status
    ) {
        log.info("👮 Getting officer incidents");
        return incidentService.getOfficerIncidents(pageableParam, status);
    }

    @Authenticated
    @AuthorizedRole({Role.POLICE_OFFICER, Role.STATION_ADMIN, Role.ROOT})
    @GraphQLQuery(name = "getNearbyIncidents", description = "Get incidents near a location")
    public Response<List<IncidentReport>> getNearbyIncidents(
            @GraphQLArgument(name = "latitude") Double latitude,
            @GraphQLArgument(name = "longitude") Double longitude,
            @GraphQLArgument(name = "radiusKm") Double radiusKm,
            @GraphQLArgument(name = "status") IncidentStatus status
    ) {
        log.info(" Getting nearby incidents");
        return incidentService.getNearbyIncidents(latitude, longitude, radiusKm, status);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @GraphQLQuery(name = "getIncidentStats", description = "Get incident statistics for station")
    public Response<IncidentReportService.IncidentStats> getIncidentStats(
            @GraphQLArgument(name = "stationUid") String stationUid){
        return incidentService.getStationStats(stationUid);
    }
}