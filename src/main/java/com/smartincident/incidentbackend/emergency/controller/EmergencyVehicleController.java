package com.smartincident.incidentbackend.emergency.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.emergency.entity.EmergencyVehicle;
import com.smartincident.incidentbackend.emergency.service.EmergencyVehicleService;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emergency/vehicles")
@RequiredArgsConstructor
public class EmergencyVehicleController {

    private final EmergencyVehicleService vehicleService;

    @Authenticated
    @RequiresPermission(Permission.VIEW_VEHICLES)
    @GetMapping("/{uid}")
    public Response<EmergencyVehicle> getVehicle(@PathVariable String uid) {
        return vehicleService.getVehicle(uid);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_VEHICLES)
    @DeleteMapping("/{uid}")
    public Response<EmergencyVehicle> deleteVehicle(@PathVariable String uid) {
        return vehicleService.deleteVehicle(uid);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_VEHICLES)
    @GetMapping
    public ResponsePage<EmergencyVehicle> getVehicles(@ModelAttribute PageableParam param) {
        return vehicleService.getVehicles(param != null ? param : new PageableParam());
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_VEHICLES)
    @GetMapping("/by-station/{stationUid}")
    public ResponsePage<EmergencyVehicle> getByStation(
            @ModelAttribute PageableParam param,
            @PathVariable String stationUid) {
        return vehicleService.getVehiclesByUnit(param != null ? param : new PageableParam(), stationUid);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_VEHICLES)
    @GetMapping("/available")
    public ResponseList<EmergencyVehicle> getAvailable(
            @RequestParam(required = false) String vehicleType) {
        return vehicleService.getAvailableVehicles(vehicleType);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_VEHICLES)
    @PutMapping("/{uid}/status")
    public Response<EmergencyVehicle> updateStatus(
            @PathVariable String uid,
            @RequestParam String status) {
        return vehicleService.updateStatus(uid, status);
    }
}
