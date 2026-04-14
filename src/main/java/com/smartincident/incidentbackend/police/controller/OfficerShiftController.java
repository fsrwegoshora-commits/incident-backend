package com.smartincident.incidentbackend.police.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.enums.Role;
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
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @PostMapping
    public Response<OfficerShift> saveShift(@RequestBody OfficerShiftDto officerShiftDto) {
        return officerShiftService.saveShift(officerShiftDto);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @PutMapping("/{uid}/excuse")
    public Response<OfficerShift> excuseShift(@PathVariable String uid, @RequestParam String reason) {
        return officerShiftService.excuseShift(uid, reason);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @DeleteMapping("/{uid}")
    public Response<OfficerShift> deleteOfficerShift(@PathVariable String uid) {
        return officerShiftService.deleteOfficerShift(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @PutMapping("/{uid}/reassign/{newOfficerUid}")
    public Response<OfficerShift> reassignShift(@PathVariable String uid, @PathVariable String newOfficerUid) {
        return officerShiftService.reassignShift(uid, newOfficerUid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @GetMapping("/checkpoint/{checkpointUid}")
    public ResponsePage<OfficerShift> getPoliceOfficerShiftsByCheckpoint(
            @ModelAttribute PageableParam pageableParam,
            @PathVariable String checkpointUid) {
        return officerShiftService.getPoliceOfficerShiftsByCheckpoint(pageableParam != null ? pageableParam : new PageableParam(), checkpointUid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @GetMapping
    public ResponsePage<OfficerShift> getPoliceOfficerShifts(@ModelAttribute PageableParam pageableParam) {
        return officerShiftService.getPoliceOfficerShifts(pageableParam != null ? pageableParam : new PageableParam());
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT, Role.POLICE_OFFICER})
    @GetMapping("/{uid}")
    public Response<OfficerShift> getPoliceOfficerShift(@PathVariable String uid) {
        return officerShiftService.getPoliceOfficerShift(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @GetMapping("/station/{policeStationUid}")
    public ResponsePage<OfficerShift> getShiftsByStation(
            @ModelAttribute PageableParam pageableParam,
            @PathVariable String policeStationUid) {
        return officerShiftService.getShiftsByStation(pageableParam != null ? pageableParam : new PageableParam(), policeStationUid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT, Role.POLICE_OFFICER})
    @GetMapping("/officer/{policeOfficerUid}")
    public ResponsePage<OfficerShift> getShiftsByPoliceOfficer(
            @ModelAttribute PageableParam pageableParam,
            @PathVariable String policeOfficerUid) {
        return officerShiftService.getShiftsByPoliceOfficer(pageableParam != null ? pageableParam : new PageableParam(), policeOfficerUid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT, Role.CITIZEN, Role.POLICE_OFFICER})
    @GetMapping("/on-duty/{stationUid}")
    public Response<OfficerShift> getCurrentOfficerOnDuty(@PathVariable String stationUid) {
        return officerShiftService.getCurrentOfficerOnDuty(stationUid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @GetMapping("/on-duty/all/{stationUid}")
    public ResponseList<OfficerShift> getAllOfficersOnDutyNow(@PathVariable String stationUid) {
        return officerShiftService.getAllOfficersOnDutyNow(stationUid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @PostMapping("/checkpoint/bulk")
    public ResponseList<OfficerShift> assignCheckpointShiftBulk(@RequestBody BulkCheckpointShiftDto bulkCheckpointShiftDto) {
        return officerShiftService.assignCheckpointShiftBulk(bulkCheckpointShiftDto);
    }
}
