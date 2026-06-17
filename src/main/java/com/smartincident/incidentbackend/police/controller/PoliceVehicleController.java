package com.smartincident.incidentbackend.police.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.police.dto.PoliceVehicleDto;
import com.smartincident.incidentbackend.police.dto.VehicleAssignmentDto;
import com.smartincident.incidentbackend.police.entity.PoliceVehicle;
import com.smartincident.incidentbackend.police.service.PoliceVehicleService;
import com.smartincident.incidentbackend.utils.PageableParam;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponsePage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/police/vehicles")
@RequiredArgsConstructor
public class PoliceVehicleController {

    private final PoliceVehicleService vehicleService;

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.AGENCY_ADMIN, Role.ROOT})
    @PostMapping
    public Response<PoliceVehicle> saveVehicle(@RequestBody PoliceVehicleDto dto) {
        return vehicleService.saveVehicle(dto);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.AGENCY_ADMIN, Role.ROOT,
                     Role.DISPATCHER, Role.DISPATCHER_SUPERVISOR, Role.DISPATCH_CENTER_ADMIN})
    @GetMapping("/{uid}")
    public Response<PoliceVehicle> getVehicle(@PathVariable String uid) {
        return vehicleService.getVehicle(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.AGENCY_ADMIN, Role.ROOT,
                     Role.DISPATCHER, Role.DISPATCHER_SUPERVISOR, Role.DISPATCH_CENTER_ADMIN})
    @GetMapping("/station/{stationUid}")
    public ResponsePage<PoliceVehicle> getByStation(
            @ModelAttribute PageableParam pageableParam,
            @PathVariable String stationUid) {
        return vehicleService.getVehiclesByStation(
                pageableParam != null ? pageableParam : new PageableParam(), stationUid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.AGENCY_ADMIN, Role.ROOT})
    @GetMapping
    public ResponsePage<PoliceVehicle> getAll(@ModelAttribute PageableParam pageableParam) {
        return vehicleService.getVehicles(pageableParam != null ? pageableParam : new PageableParam());
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.AGENCY_ADMIN, Role.ROOT})
    @PatchMapping("/{uid}/assign")
    public Response<PoliceVehicle> assignVehicle(
            @PathVariable String uid,
            @RequestBody VehicleAssignmentDto dto) {
        return vehicleService.assignVehicle(uid, dto);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.AGENCY_ADMIN, Role.ROOT})
    @PutMapping("/{uid}/status")
    public Response<PoliceVehicle> updateStatus(
            @PathVariable String uid,
            @RequestParam String status) {
        return vehicleService.updateStatus(uid, status);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.AGENCY_ADMIN, Role.ROOT})
    @DeleteMapping("/{uid}")
    public Response<PoliceVehicle> deleteVehicle(@PathVariable String uid) {
        return vehicleService.deleteVehicle(uid);
    }
}
