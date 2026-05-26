package com.smartincident.incidentbackend.emergency.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.emergency.dto.VehicleShiftDto;
import com.smartincident.incidentbackend.emergency.entity.VehicleShift;
import com.smartincident.incidentbackend.emergency.service.VehicleShiftService;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emergency/vehicle-shifts")
@RequiredArgsConstructor
public class VehicleShiftController {

    private final VehicleShiftService shiftService;

    @Authenticated
    @RequiresPermission(Permission.MANAGE_VEHICLES)
    @PostMapping
    public Response<VehicleShift> saveShift(@RequestBody VehicleShiftDto dto) {
        return shiftService.saveShift(dto);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_VEHICLES)
    @DeleteMapping("/{uid}")
    public Response<VehicleShift> deleteShift(@PathVariable String uid) {
        return shiftService.deleteShift(uid);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_VEHICLES)
    @GetMapping("/station/{stationUid}")
    public ResponsePage<VehicleShift> getByStation(
            @ModelAttribute PageableParam param,
            @PathVariable String stationUid) {
        return shiftService.getShiftsByStation(param != null ? param : new PageableParam(), stationUid);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_VEHICLES)
    @GetMapping("/vehicle/{vehicleUid}")
    public ResponsePage<VehicleShift> getByVehicle(
            @ModelAttribute PageableParam param,
            @PathVariable String vehicleUid) {
        return shiftService.getShiftsByVehicle(param != null ? param : new PageableParam(), vehicleUid);
    }
}
