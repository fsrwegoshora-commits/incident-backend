package com.smartincident.incidentbackend.emergency.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.emergency.dto.DispatchDto;
import com.smartincident.incidentbackend.emergency.entity.IncidentDispatch;
import com.smartincident.incidentbackend.emergency.service.IncidentDispatchService;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emergency/dispatch")
@RequiredArgsConstructor
public class IncidentDispatchController {

    private final IncidentDispatchService dispatchService;

    /** Dispatch a vehicle to an incident */
    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.FIRE_STATION_ADMIN, Role.MEDICAL_STATION_ADMIN, Role.ROOT})
    @PostMapping
    public Response<IncidentDispatch> dispatch(@RequestBody DispatchDto dto) {
        return dispatchService.dispatch(dto);
    }

    /** Crew updates dispatch status (acknowledged → en_route → on_scene → cleared) */
    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.FIRE_STATION_ADMIN, Role.MEDICAL_STATION_ADMIN,
                     Role.FIRE_OFFICER, Role.MEDIC, Role.ROOT})
    @PutMapping("/{uid}/status")
    public Response<IncidentDispatch> updateStatus(
            @PathVariable String uid,
            @RequestBody DispatchDto dto) {
        return dispatchService.updateStatus(uid, dto);
    }

    /** All dispatches for a given incident */
    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.FIRE_STATION_ADMIN, Role.MEDICAL_STATION_ADMIN,
                     Role.POLICE_OFFICER, Role.FIRE_OFFICER, Role.MEDIC, Role.ROOT})
    @GetMapping("/incident/{incidentUid}")
    public ResponseList<IncidentDispatch> getByIncident(@PathVariable String incidentUid) {
        return dispatchService.getByIncident(incidentUid);
    }

    /** Dispatch history for a specific vehicle */
    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.FIRE_STATION_ADMIN, Role.MEDICAL_STATION_ADMIN, Role.ROOT})
    @GetMapping("/vehicle/{vehicleUid}")
    public ResponseList<IncidentDispatch> getByVehicle(@PathVariable String vehicleUid) {
        return dispatchService.getByVehicle(vehicleUid);
    }

    /** All active (non-cleared) dispatches for caller's station */
    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.FIRE_STATION_ADMIN, Role.MEDICAL_STATION_ADMIN, Role.ROOT})
    @GetMapping("/active")
    public ResponseList<IncidentDispatch> getActive(
            @RequestParam(required = false) String stationUid) {
        return dispatchService.getActiveByStation(stationUid);
    }
}
