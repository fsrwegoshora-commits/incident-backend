package com.smartincident.incidentbackend.dispatcher.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.dispatcher.dto.DispatcherShiftDto;
import com.smartincident.incidentbackend.dispatcher.entity.DispatcherShift;
import com.smartincident.incidentbackend.dispatcher.service.DispatcherShiftService;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponseList;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dispatcher/shifts")
@RequiredArgsConstructor
public class DispatcherShiftController {

    private final DispatcherShiftService dispatcherShiftService;

    @Authenticated
    @RequiresPermission(Permission.VIEW_DISPATCH_CENTER)
    @PostMapping
    public Response<DispatcherShift> saveShift(@RequestBody DispatcherShiftDto dto) {
        return dispatcherShiftService.saveShift(dto);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_DISPATCH_CENTER)
    @DeleteMapping("/{uid}")
    public Response<DispatcherShift> deleteShift(@PathVariable String uid) {
        return dispatcherShiftService.deleteShift(uid);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_SLA_METRICS)
    @GetMapping("/date/{date}")
    public ResponseList<DispatcherShift> getShiftsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dispatcherShiftService.getShiftsByDate(date);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_SLA_METRICS)
    @GetMapping("/dispatcher/{dispatcherUid}")
    public ResponseList<DispatcherShift> getShiftsByDispatcher(@PathVariable String dispatcherUid) {
        return dispatcherShiftService.getShiftsByDispatcher(dispatcherUid);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_SLA_METRICS)
    @GetMapping("/on-duty")
    public ResponseList<DispatcherShift> getOnDutyNow() {
        return dispatcherShiftService.getOnDutyNow();
    }
}
