package com.smartincident.incidentbackend.emergency.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.emergency.dto.VehicleShiftDto;
import com.smartincident.incidentbackend.emergency.entity.VehicleShift;
import com.smartincident.incidentbackend.emergency.service.VehicleShiftService;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emergency/vehicle-shifts")
@RequiredArgsConstructor
public class VehicleShiftController {

    private final VehicleShiftService shiftService;

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.FIRE_STATION_ADMIN, Role.MEDICAL_STATION_ADMIN, Role.ROOT})
    @PostMapping
    public Response<VehicleShift> saveShift(@RequestBody VehicleShiftDto dto) {
        return shiftService.saveShift(dto);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.FIRE_STATION_ADMIN, Role.MEDICAL_STATION_ADMIN, Role.ROOT})
    @DeleteMapping("/{uid}")
    public Response<VehicleShift> deleteShift(@PathVariable String uid) {
        return shiftService.deleteShift(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.FIRE_STATION_ADMIN, Role.MEDICAL_STATION_ADMIN, Role.ROOT})
    @GetMapping("/station/{stationUid}")
    public ResponsePage<VehicleShift> getByStation(
            @ModelAttribute PageableParam param,
            @PathVariable String stationUid) {
        return shiftService.getShiftsByStation(param != null ? param : new PageableParam(), stationUid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.FIRE_STATION_ADMIN, Role.MEDICAL_STATION_ADMIN,
                     Role.FIRE_OFFICER, Role.MEDIC, Role.ROOT})
    @GetMapping("/vehicle/{vehicleUid}")
    public ResponsePage<VehicleShift> getByVehicle(
            @ModelAttribute PageableParam param,
            @PathVariable String vehicleUid) {
        return shiftService.getShiftsByVehicle(param != null ? param : new PageableParam(), vehicleUid);
    }
}
