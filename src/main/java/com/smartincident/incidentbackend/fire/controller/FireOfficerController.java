package com.smartincident.incidentbackend.fire.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.fire.dto.FireOfficerDto;
import com.smartincident.incidentbackend.fire.entity.FireOfficer;
import com.smartincident.incidentbackend.fire.service.FireOfficerService;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fire/officers")
@RequiredArgsConstructor
public class FireOfficerController {

    private final FireOfficerService officerService;

    @Authenticated
    @RequiresPermission(Permission.MANAGE_STATIONS)
    @PostMapping
    public Response<FireOfficer> save(@RequestBody FireOfficerDto dto) {
        return officerService.saveOfficer(dto);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_RESPONDERS)
    @GetMapping("/{uid}")
    public Response<FireOfficer> get(@PathVariable String uid) {
        return officerService.getOfficer(uid);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_RESPONDERS)
    @GetMapping
    public ResponsePage<FireOfficer> list(
            @ModelAttribute PageableParam param,
            @RequestParam(required = false) String unitUid) {
        return officerService.getOfficers(param != null ? param : new PageableParam(), unitUid);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_STATIONS)
    @DeleteMapping("/{uid}")
    public Response<FireOfficer> delete(@PathVariable String uid) {
        return officerService.deleteOfficer(uid);
    }

    @Authenticated
    @RequiresPermission(Permission.UPDATE_INCIDENT)
    @PutMapping("/{uid}/duty")
    public Response<FireOfficer> duty(
            @PathVariable String uid,
            @RequestParam boolean onDuty) {
        return officerService.toggleDuty(uid, onDuty);
    }
}
