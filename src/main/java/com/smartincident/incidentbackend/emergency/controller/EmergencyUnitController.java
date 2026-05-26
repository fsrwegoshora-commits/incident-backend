package com.smartincident.incidentbackend.emergency.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.emergency.dto.EmergencyUnitDto;
import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.emergency.service.EmergencyUnitService;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.enums.UnitLevel;
import com.smartincident.incidentbackend.enums.UnitType;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emergency/units")
@RequiredArgsConstructor
public class EmergencyUnitController {

    private final EmergencyUnitService unitService;

    @Authenticated
    @RequiresPermission(Permission.MANAGE_STATIONS)
    @PostMapping
    public Response<EmergencyUnit> save(@RequestBody EmergencyUnitDto dto) {
        return unitService.saveUnit(dto);
    }

    @Authenticated
    @GetMapping("/{uid}")
    public Response<EmergencyUnit> get(@PathVariable String uid) {
        return unitService.getUnit(uid);
    }

    @Authenticated
    @GetMapping
    public ResponsePage<EmergencyUnit> list(
            @ModelAttribute PageableParam param,
            @RequestParam(required = false) String agencyUid,
            @RequestParam(required = false) UnitType unitType,
            @RequestParam(required = false) UnitLevel level) {
        return unitService.getUnits(param != null ? param : new PageableParam(), agencyUid, unitType, level);
    }

    @Authenticated
    @GetMapping("/by-agency/{agencyUid}")
    public ResponseList<EmergencyUnit> byAgency(@PathVariable String agencyUid) {
        return unitService.getByAgency(agencyUid);
    }

    @Authenticated
    @GetMapping("/{uid}/children")
    public ResponseList<EmergencyUnit> children(@PathVariable String uid) {
        return unitService.getChildren(uid);
    }

    @Authenticated
    @GetMapping("/nearby")
    public ResponseList<EmergencyUnit> nearby(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "20") double radiusKm,
            @RequestParam(required = false) UnitType unitType) {
        return unitService.getNearby(latitude, longitude, radiusKm, unitType);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_STATIONS)
    @DeleteMapping("/{uid}")
    public Response<EmergencyUnit> delete(@PathVariable String uid) {
        return unitService.deleteUnit(uid);
    }
}
