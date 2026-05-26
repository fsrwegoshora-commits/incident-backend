package com.smartincident.incidentbackend.emergency.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.emergency.dto.DispatchDto;
import com.smartincident.incidentbackend.emergency.entity.IncidentDispatch;
import com.smartincident.incidentbackend.emergency.service.IncidentDispatchService;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emergency/dispatch")
@RequiredArgsConstructor
public class IncidentDispatchController {

    private final IncidentDispatchService dispatchService;

    @Authenticated
    @RequiresPermission(Permission.DISPATCH_INCIDENT)
    @PostMapping
    public Response<IncidentDispatch> dispatch(@RequestBody DispatchDto dto) {
        return dispatchService.dispatch(dto);
    }

    @Authenticated
    @RequiresPermission(Permission.UPDATE_INCIDENT)
    @PutMapping("/{uid}/status")
    public Response<IncidentDispatch> updateStatus(@PathVariable String uid,
                                                   @RequestBody DispatchDto dto) {
        return dispatchService.updateStatus(uid, dto);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_VEHICLES)
    @GetMapping("/incident/{incidentUid}")
    public ResponseList<IncidentDispatch> getByIncident(@PathVariable String incidentUid) {
        return dispatchService.getByIncident(incidentUid);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_VEHICLES)
    @GetMapping("/vehicle/{vehicleUid}")
    public ResponseList<IncidentDispatch> getByVehicle(@PathVariable String vehicleUid) {
        return dispatchService.getByVehicle(vehicleUid);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_VEHICLES)
    @GetMapping("/active")
    public ResponseList<IncidentDispatch> getActive(
            @RequestParam(required = false) String stationUid) {
        return dispatchService.getActiveByStation(stationUid);
    }
}
