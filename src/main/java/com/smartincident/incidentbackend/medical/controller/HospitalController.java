package com.smartincident.incidentbackend.medical.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.enums.HospitalType;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.medical.dto.HospitalDto;
import com.smartincident.incidentbackend.medical.entity.Hospital;
import com.smartincident.incidentbackend.medical.service.HospitalService;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/medical/hospitals")
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalService hospitalService;

    @Authenticated
    @RequiresPermission(Permission.MANAGE_STATIONS)
    @PostMapping
    public Response<Hospital> save(@RequestBody HospitalDto dto) {
        return hospitalService.saveHospital(dto);
    }

    @Authenticated
    @GetMapping("/{uid}")
    public Response<Hospital> get(@PathVariable String uid) {
        return hospitalService.getHospital(uid);
    }

    @Authenticated
    @GetMapping
    public ResponsePage<Hospital> list(
            @ModelAttribute PageableParam param,
            @RequestParam(required = false) HospitalType hospitalType) {
        return hospitalService.getHospitals(param != null ? param : new PageableParam(), hospitalType);
    }

    @Authenticated
    @GetMapping("/nearby")
    public ResponseList<Hospital> nearby(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "20") double radiusKm) {
        return hospitalService.getNearby(latitude, longitude, radiusKm);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_STATIONS)
    @DeleteMapping("/{uid}")
    public Response<Hospital> delete(@PathVariable String uid) {
        return hospitalService.deleteHospital(uid);
    }
}
