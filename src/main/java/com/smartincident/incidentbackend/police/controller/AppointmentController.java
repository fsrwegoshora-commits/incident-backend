package com.smartincident.incidentbackend.police.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.enums.AppointmentStatus;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.police.dto.AppointmentDto;
import com.smartincident.incidentbackend.police.entity.StationAppointment;
import com.smartincident.incidentbackend.police.service.AppointmentService;
import com.smartincident.incidentbackend.utils.PageableParam;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponseList;
import com.smartincident.incidentbackend.utils.ResponsePage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Authenticated
    @RequiresPermission(Permission.MANAGE_STATIONS)
    @PostMapping
    public Response<StationAppointment> saveAppointment(@RequestBody AppointmentDto dto) {
        return appointmentService.saveAppointment(dto);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_STATION_APPOINTMENTS)
    @GetMapping("/station/{stationUid}")
    public ResponsePage<StationAppointment> getByStation(
            @ModelAttribute PageableParam pageableParam,
            @PathVariable String stationUid,
            @RequestParam(required = false) AppointmentStatus status) {
        return appointmentService.getByStation(
                pageableParam != null ? pageableParam : new PageableParam(), stationUid, status);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_STATION_APPOINTMENTS)
    @GetMapping("/station/{stationUid}/active")
    public ResponseList<StationAppointment> getActiveByStation(@PathVariable String stationUid) {
        return appointmentService.getActiveByStation(stationUid);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_OFFICER_APPOINTMENTS)
    @GetMapping("/officer/{officerUid}")
    public ResponseList<StationAppointment> getByOfficer(@PathVariable String officerUid) {
        return appointmentService.getByOfficer(officerUid);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_STATIONS)
    @PatchMapping("/{uid}/status")
    public Response<StationAppointment> updateStatus(
            @PathVariable String uid,
            @RequestParam AppointmentStatus status) {
        return appointmentService.updateStatus(uid, status);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_STATIONS)
    @DeleteMapping("/{uid}")
    public Response<StationAppointment> deleteAppointment(@PathVariable String uid) {
        return appointmentService.deleteAppointment(uid);
    }
}
