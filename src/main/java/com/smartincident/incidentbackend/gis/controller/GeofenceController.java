package com.smartincident.incidentbackend.gis.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.enums.GeofenceType;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.gis.dto.GeofenceDto;
import com.smartincident.incidentbackend.gis.entity.Geofence;
import com.smartincident.incidentbackend.gis.service.GeofenceService;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponseList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/geofences")
@RequiredArgsConstructor
@Slf4j
public class GeofenceController {

    private final GeofenceService geofenceService;

    @Authenticated
    @AuthorizedRole({Role.DISPATCHER, Role.DISPATCHER_SUPERVISOR, Role.DISPATCH_CENTER_ADMIN,
                     Role.STATION_ADMIN, Role.AGENCY_ADMIN, Role.ROOT})
    @GetMapping
    public ResponseList<Geofence> getAll(
            @RequestParam(required = false) String agencyUid,
            @RequestParam(required = false) GeofenceType type) {
        return geofenceService.getAll(agencyUid, type);
    }

    @Authenticated
    @AuthorizedRole({Role.DISPATCHER, Role.DISPATCHER_SUPERVISOR, Role.DISPATCH_CENTER_ADMIN,
                     Role.STATION_ADMIN, Role.AGENCY_ADMIN, Role.ROOT})
    @GetMapping("/{uid}")
    public Response<Geofence> getByUid(@PathVariable String uid) {
        return geofenceService.getByUid(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.DISPATCH_CENTER_ADMIN, Role.AGENCY_ADMIN, Role.ROOT})
    @PostMapping
    public Response<Geofence> create(@RequestBody GeofenceDto dto) {
        log.info("Creating geofence: {}", dto.getName());
        return geofenceService.create(dto);
    }

    @Authenticated
    @AuthorizedRole({Role.DISPATCH_CENTER_ADMIN, Role.AGENCY_ADMIN, Role.ROOT})
    @PutMapping("/{uid}")
    public Response<Geofence> update(@PathVariable String uid, @RequestBody GeofenceDto dto) {
        log.info("Updating geofence: {}", uid);
        return geofenceService.update(uid, dto);
    }

    @Authenticated
    @AuthorizedRole({Role.DISPATCH_CENTER_ADMIN, Role.AGENCY_ADMIN, Role.ROOT})
    @DeleteMapping("/{uid}")
    public Response<Geofence> delete(@PathVariable String uid) {
        log.info("Deleting geofence: {}", uid);
        return geofenceService.delete(uid);
    }

    /**
     * Check which active geofences contain the given coordinates.
     * Called by mobile clients when a vehicle or responder updates their GPS.
     */
    @Authenticated
    @GetMapping("/check")
    public ResponseList<Geofence> checkPoint(
            @RequestParam double lat,
            @RequestParam double lng) {
        return geofenceService.checkPoint(lat, lng);
    }
}
