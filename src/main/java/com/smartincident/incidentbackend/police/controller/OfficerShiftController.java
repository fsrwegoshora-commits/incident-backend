package com.smartincident.incidentbackend.police.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.police.dto.BulkCheckpointShiftDto;
import com.smartincident.incidentbackend.police.dto.OfficerShiftDto;
import com.smartincident.incidentbackend.police.entity.OfficerShift;
import com.smartincident.incidentbackend.police.service.OfficerShiftService;
import com.smartincident.incidentbackend.utils.PageableParam;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponseList;
import com.smartincident.incidentbackend.utils.ResponsePage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/police/shifts")
@RequiredArgsConstructor
public class OfficerShiftController {
    private final OfficerShiftService officerShiftService;

    @Authenticated
    @RequiresPermission(Permission.MANAGE_OFFICER_SHIFTS)
    @PostMapping
    public Response<OfficerShift> saveShift(@RequestBody OfficerShiftDto officerShiftDto) {
        return officerShiftService.saveShift(officerShiftDto);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_OFFICER_SHIFTS)
    @PutMapping("/{uid}/excuse")
    public Response<OfficerShift> excuseShift(@PathVariable String uid, @RequestParam String reason) {
        return officerShiftService.excuseShift(uid, reason);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_OFFICER_SHIFTS)
    @DeleteMapping("/{uid}")
    public Response<OfficerShift> deleteOfficerShift(@PathVariable String uid) {
        return officerShiftService.deleteOfficerShift(uid);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_OFFICER_SHIFTS)
    @PutMapping("/{uid}/reassign/{newOfficerUid}")
    public Response<OfficerShift> reassignShift(@PathVariable String uid, @PathVariable String newOfficerUid) {
        return officerShiftService.reassignShift(uid, newOfficerUid);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_OFFICER_SHIFTS)
    @GetMapping("/checkpoint/{checkpointUid}")
    public ResponsePage<OfficerShift> getPoliceOfficerShiftsByCheckpoint(
            @ModelAttribute PageableParam pageableParam,
            @PathVariable String checkpointUid) {
        return officerShiftService.getPoliceOfficerShiftsByCheckpoint(pageableParam != null ? pageableParam : new PageableParam(), checkpointUid);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_OFFICER_SHIFTS)
    @GetMapping
    public ResponsePage<OfficerShift> getPoliceOfficerShifts(@ModelAttribute PageableParam pageableParam) {
        return officerShiftService.getPoliceOfficerShifts(pageableParam != null ? pageableParam : new PageableParam());
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_OWN_OFFICER_SHIFT)
    @GetMapping("/{uid}")
    public Response<OfficerShift> getPoliceOfficerShift(@PathVariable String uid) {
        return officerShiftService.getPoliceOfficerShift(uid);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_OFFICER_SHIFTS)
    @GetMapping("/station/{policeStationUid}")
    public ResponsePage<OfficerShift> getShiftsByStation(
            @ModelAttribute PageableParam pageableParam,
            @PathVariable String policeStationUid) {
        return officerShiftService.getShiftsByStation(pageableParam != null ? pageableParam : new PageableParam(), policeStationUid);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_OWN_OFFICER_SHIFT)
    @GetMapping("/officer/{policeOfficerUid}")
    public ResponsePage<OfficerShift> getShiftsByPoliceOfficer(
            @ModelAttribute PageableParam pageableParam,
            @PathVariable String policeOfficerUid) {
        return officerShiftService.getShiftsByPoliceOfficer(pageableParam != null ? pageableParam : new PageableParam(), policeOfficerUid);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_OFFICER_ON_DUTY)
    @GetMapping("/on-duty/{stationUid}")
    public Response<OfficerShift> getCurrentOfficerOnDuty(@PathVariable String stationUid) {
        return officerShiftService.getCurrentOfficerOnDuty(stationUid);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_OFFICER_SHIFTS)
    @GetMapping("/on-duty/all/{stationUid}")
    public ResponseList<OfficerShift> getAllOfficersOnDutyNow(@PathVariable String stationUid) {
        return officerShiftService.getAllOfficersOnDutyNow(stationUid);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_OFFICER_SHIFTS)
    @PostMapping("/checkpoint/bulk")
    public ResponseList<OfficerShift> assignCheckpointShiftBulk(@RequestBody BulkCheckpointShiftDto bulkCheckpointShiftDto) {
        return officerShiftService.assignCheckpointShiftBulk(bulkCheckpointShiftDto);
    }
}
