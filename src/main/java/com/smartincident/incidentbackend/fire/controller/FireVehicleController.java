package com.smartincident.incidentbackend.fire.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.fire.dto.FireVehicleDto;
import com.smartincident.incidentbackend.fire.entity.FireVehicle;
import com.smartincident.incidentbackend.fire.service.FireVehicleService;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fire/vehicles")
@RequiredArgsConstructor
public class FireVehicleController {

    private final FireVehicleService vehicleService;

    @Authenticated
    @RequiresPermission(Permission.MANAGE_VEHICLES)
    @PostMapping
    public Response<FireVehicle> saveVehicle(@RequestBody FireVehicleDto dto) {
        return vehicleService.saveVehicle(dto);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_VEHICLES)
    @GetMapping("/{uid}")
    public Response<FireVehicle> getVehicle(@PathVariable String uid) {
        return vehicleService.getVehicle(uid);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_VEHICLES)
    @DeleteMapping("/{uid}")
    public Response<FireVehicle> deleteVehicle(@PathVariable String uid) {
        return vehicleService.deleteVehicle(uid);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_VEHICLES)
    @GetMapping
    public ResponsePage<FireVehicle> getVehicles(@ModelAttribute PageableParam param) {
        return vehicleService.getVehicles(param != null ? param : new PageableParam());
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_VEHICLES)
    @GetMapping("/by-unit/{unitUid}")
    public ResponsePage<FireVehicle> getByUnit(
            @ModelAttribute PageableParam param,
            @PathVariable String unitUid) {
        return vehicleService.getVehiclesByUnit(param != null ? param : new PageableParam(), unitUid);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_VEHICLES)
    @GetMapping("/available")
    public ResponseList<FireVehicle> getAvailable() {
        return vehicleService.getAvailableVehicles();
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_VEHICLES)
    @PutMapping("/{uid}/status")
    public Response<FireVehicle> updateStatus(
            @PathVariable String uid,
            @RequestParam String status) {
        return vehicleService.updateStatus(uid, status);
    }
}
