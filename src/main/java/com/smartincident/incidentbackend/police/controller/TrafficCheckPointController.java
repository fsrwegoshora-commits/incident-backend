package com.smartincident.incidentbackend.police.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.police.dto.TrafficCheckPointDto;
import com.smartincident.incidentbackend.police.entity.TrafficCheckpoint;
import com.smartincident.incidentbackend.police.service.TrafficCheckPointService;
import com.smartincident.incidentbackend.utils.PageableParam;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponsePage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/police/checkpoints")
@RequiredArgsConstructor
public class TrafficCheckPointController {
    private final TrafficCheckPointService trafficCheckPointService;

    @Authenticated
    @RequiresPermission(Permission.MANAGE_CHECKPOINTS)
    @PostMapping
    public Response<TrafficCheckpoint> saveTrafficCheckpoint(@RequestBody TrafficCheckPointDto trafficCheckPointDto) {
        return trafficCheckPointService.saveTrafficCheckpoint(trafficCheckPointDto);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_CHECKPOINTS)
    @DeleteMapping("/{uid}")
    public Response<TrafficCheckpoint> deleteTrafficCheckpoint(@PathVariable String uid) {
        return trafficCheckPointService.deleteTrafficCheckpoint(uid);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_CHECKPOINTS)
    @GetMapping("/{uid}")
    public Response<TrafficCheckpoint> getTrafficCheckpoint(@PathVariable String uid) {
        return trafficCheckPointService.getTrafficCheckpointByUid(uid);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_CHECKPOINTS)
    @GetMapping
    public ResponsePage<TrafficCheckpoint> getTrafficCheckpoints(@ModelAttribute PageableParam pageableParam) {
        return trafficCheckPointService.getCheckpoints(pageableParam != null ? pageableParam : new PageableParam());
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_CHECKPOINTS)
    @GetMapping("/station/{stationUid}")
    public ResponsePage<TrafficCheckpoint> getTrafficCheckpointsByPoliceStation(
            @ModelAttribute PageableParam pageableParam,
            @PathVariable String stationUid) {
        return trafficCheckPointService.getTrafficCheckpointsByPoliceStation(pageableParam != null ? pageableParam : new PageableParam(), stationUid);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_CHECKPOINTS)
    @PutMapping("/{checkpointUid}/assign-supervisor/{officerUid}")
    public Response<TrafficCheckpoint> assignSupervisor(
            @PathVariable String checkpointUid,
            @PathVariable String officerUid) {
        return trafficCheckPointService.assignSupervisor(checkpointUid, officerUid);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_CHECKPOINTS)
    @PutMapping("/{checkpointUid}/change-supervisor/{newOfficerUid}")
    public Response<TrafficCheckpoint> changeSupervisor(
            @PathVariable String checkpointUid,
            @PathVariable String newOfficerUid) {
        return trafficCheckPointService.changeSupervisor(checkpointUid, newOfficerUid);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_CHECKPOINTS)
    @PutMapping("/{checkpointUid}/toggle")
    public Response<TrafficCheckpoint> activateOrDeactivateCheckpoint(@PathVariable String checkpointUid) {
        return trafficCheckPointService.activateOrDeactivateCheckpoint(checkpointUid);
    }
}
