package com.smartincident.incidentbackend.emergency.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.emergency.dto.EmergencyVehicleDto;
import com.smartincident.incidentbackend.emergency.entity.EmergencyVehicle;
import com.smartincident.incidentbackend.emergency.service.EmergencyVehicleService;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emergency/vehicles")
@RequiredArgsConstructor
public class EmergencyVehicleController {

    private final EmergencyVehicleService vehicleService;

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.FIRE_STATION_ADMIN, Role.MEDICAL_STATION_ADMIN, Role.ROOT})
    @PostMapping
    public Response<EmergencyVehicle> saveVehicle(@RequestBody EmergencyVehicleDto dto) {
        return vehicleService.saveVehicle(dto);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.FIRE_STATION_ADMIN, Role.MEDICAL_STATION_ADMIN, Role.ROOT})
    @GetMapping("/{uid}")
    public Response<EmergencyVehicle> getVehicle(@PathVariable String uid) {
        return vehicleService.getVehicle(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.FIRE_STATION_ADMIN, Role.MEDICAL_STATION_ADMIN, Role.ROOT})
    @DeleteMapping("/{uid}")
    public Response<EmergencyVehicle> deleteVehicle(@PathVariable String uid) {
        return vehicleService.deleteVehicle(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.ROOT})
    @GetMapping
    public ResponsePage<EmergencyVehicle> getVehicles(@ModelAttribute PageableParam param) {
        return vehicleService.getVehicles(param != null ? param : new PageableParam());
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.FIRE_STATION_ADMIN, Role.MEDICAL_STATION_ADMIN, Role.ROOT})
    @GetMapping("/by-station/{stationUid}")
    public ResponsePage<EmergencyVehicle> getByStation(
            @ModelAttribute PageableParam param,
            @PathVariable String stationUid) {
        return vehicleService.getVehiclesByStation(param != null ? param : new PageableParam(), stationUid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.FIRE_STATION_ADMIN, Role.MEDICAL_STATION_ADMIN, Role.ROOT})
    @GetMapping("/available")
    public ResponseList<EmergencyVehicle> getAvailable(
            @RequestParam(required = false) String vehicleType) {
        return vehicleService.getAvailableVehicles(vehicleType);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.FIRE_STATION_ADMIN, Role.MEDICAL_STATION_ADMIN, Role.ROOT})
    @PutMapping("/{uid}/status")
    public Response<EmergencyVehicle> updateStatus(
            @PathVariable String uid,
            @RequestParam String status) {
        return vehicleService.updateStatus(uid, status);
    }
}
