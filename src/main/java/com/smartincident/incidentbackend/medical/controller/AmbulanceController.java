package com.smartincident.incidentbackend.medical.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.medical.dto.AmbulanceDto;
import com.smartincident.incidentbackend.medical.entity.Ambulance;
import com.smartincident.incidentbackend.medical.service.AmbulanceService;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/medical/ambulances")
@RequiredArgsConstructor
public class AmbulanceController {

    private final AmbulanceService ambulanceService;

    @Authenticated
    @RequiresPermission(Permission.MANAGE_VEHICLES)
    @PostMapping
    public Response<Ambulance> saveAmbulance(@RequestBody AmbulanceDto dto) {
        return ambulanceService.saveAmbulance(dto);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_VEHICLES)
    @GetMapping("/{uid}")
    public Response<Ambulance> getAmbulance(@PathVariable String uid) {
        return ambulanceService.getAmbulance(uid);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_VEHICLES)
    @DeleteMapping("/{uid}")
    public Response<Ambulance> deleteAmbulance(@PathVariable String uid) {
        return ambulanceService.deleteAmbulance(uid);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_VEHICLES)
    @GetMapping
    public ResponsePage<Ambulance> getAmbulances(@ModelAttribute PageableParam param) {
        return ambulanceService.getAmbulances(param != null ? param : new PageableParam());
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_VEHICLES)
    @GetMapping("/by-unit/{unitUid}")
    public ResponsePage<Ambulance> getByUnit(
            @ModelAttribute PageableParam param,
            @PathVariable String unitUid) {
        return ambulanceService.getAmbulancesByUnit(param != null ? param : new PageableParam(), unitUid);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_VEHICLES)
    @GetMapping("/by-hospital/{hospitalUid}")
    public ResponseList<Ambulance> getByHospital(@PathVariable String hospitalUid) {
        return ambulanceService.getAmbulancesByHospital(hospitalUid);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_VEHICLES)
    @GetMapping("/available")
    public ResponseList<Ambulance> getAvailable() {
        return ambulanceService.getAvailableAmbulances();
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_VEHICLES)
    @PutMapping("/{uid}/status")
    public Response<Ambulance> updateStatus(
            @PathVariable String uid,
            @RequestParam String status) {
        return ambulanceService.updateStatus(uid, status);
    }
}
