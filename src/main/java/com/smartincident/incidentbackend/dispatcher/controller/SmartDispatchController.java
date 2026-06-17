package com.smartincident.incidentbackend.dispatcher.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.dispatcher.dto.SmartDispatchRecommendation;
import com.smartincident.incidentbackend.dispatcher.service.SmartDispatchService;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.utils.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dispatch")
@RequiredArgsConstructor
@Slf4j
public class SmartDispatchController {

    private final SmartDispatchService smartDispatchService;

    /**
     * Smart dispatch recommendations for an incident.
     * Returns ranked police stations, fire units and medical units
     * scored by distance, crew availability and current workload.
     */
    @Authenticated
    @AuthorizedRole({Role.DISPATCHER, Role.DISPATCHER_SUPERVISOR, Role.DISPATCH_CENTER_ADMIN,
                     Role.AGENCY_ADMIN, Role.ROOT})
    @GetMapping("/smart-recommend/{incidentUid}")
    public Response<SmartDispatchRecommendation> recommend(@PathVariable String incidentUid) {
        log.info("Smart dispatch recommendation requested for incident: {}", incidentUid);
        return smartDispatchService.recommend(incidentUid);
    }

    /**
     * ETA estimate from a specific vehicle's last known GPS position to given coordinates.
     */
    @Authenticated
    @AuthorizedRole({Role.DISPATCHER, Role.DISPATCHER_SUPERVISOR, Role.DISPATCH_CENTER_ADMIN,
                     Role.AGENCY_ADMIN, Role.ROOT, Role.STATION_ADMIN})
    @GetMapping("/eta")
    public Response<Map<String, Object>> getEta(
            @RequestParam String vehicleUid,
            @RequestParam double lat,
            @RequestParam double lng) {
        return smartDispatchService.getEta(vehicleUid, lat, lng);
    }
}
