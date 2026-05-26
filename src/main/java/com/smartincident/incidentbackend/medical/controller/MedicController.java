package com.smartincident.incidentbackend.medical.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.medical.dto.MedicDto;
import com.smartincident.incidentbackend.medical.entity.Medic;
import com.smartincident.incidentbackend.medical.service.MedicService;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/medical/medics")
@RequiredArgsConstructor
public class MedicController {

    private final MedicService medicService;

    @Authenticated
    @RequiresPermission(Permission.MANAGE_STATIONS)
    @PostMapping
    public Response<Medic> save(@RequestBody MedicDto dto) {
        return medicService.saveMedic(dto);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_RESPONDERS)
    @GetMapping("/{uid}")
    public Response<Medic> get(@PathVariable String uid) {
        return medicService.getMedic(uid);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_RESPONDERS)
    @GetMapping
    public ResponsePage<Medic> list(
            @ModelAttribute PageableParam param,
            @RequestParam(required = false) String hospitalUid) {
        return medicService.getMedics(param != null ? param : new PageableParam(), hospitalUid);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_STATIONS)
    @DeleteMapping("/{uid}")
    public Response<Medic> delete(@PathVariable String uid) {
        return medicService.deleteMedic(uid);
    }

    @Authenticated
    @RequiresPermission(Permission.UPDATE_INCIDENT)
    @PutMapping("/{uid}/duty")
    public Response<Medic> duty(
            @PathVariable String uid,
            @RequestParam boolean onDuty) {
        return medicService.toggleDuty(uid, onDuty);
    }
}
