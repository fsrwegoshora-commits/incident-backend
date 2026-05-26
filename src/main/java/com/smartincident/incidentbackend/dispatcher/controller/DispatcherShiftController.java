package com.smartincident.incidentbackend.dispatcher.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.dispatcher.dto.DispatcherShiftDto;
import com.smartincident.incidentbackend.dispatcher.entity.DispatcherShift;
import com.smartincident.incidentbackend.dispatcher.service.DispatcherShiftService;
import com.smartincident.incidentbackend.enums.Role;
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
    @AuthorizedRole({Role.ROOT, Role.AGENCY_ADMIN, Role.DISPATCH_CENTER_ADMIN, Role.DISPATCHER_SUPERVISOR})
    @PostMapping
    public Response<DispatcherShift> saveShift(@RequestBody DispatcherShiftDto dto) {
        return dispatcherShiftService.saveShift(dto);
    }

    @Authenticated
    @AuthorizedRole({Role.ROOT, Role.AGENCY_ADMIN, Role.DISPATCH_CENTER_ADMIN, Role.DISPATCHER_SUPERVISOR})
    @DeleteMapping("/{uid}")
    public Response<DispatcherShift> deleteShift(@PathVariable String uid) {
        return dispatcherShiftService.deleteShift(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.ROOT, Role.AGENCY_ADMIN, Role.DISPATCH_CENTER_ADMIN, Role.DISPATCHER_SUPERVISOR, Role.DISPATCHER})
    @GetMapping("/date/{date}")
    public ResponseList<DispatcherShift> getShiftsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dispatcherShiftService.getShiftsByDate(date);
    }

    @Authenticated
    @AuthorizedRole({Role.ROOT, Role.AGENCY_ADMIN, Role.DISPATCH_CENTER_ADMIN, Role.DISPATCHER_SUPERVISOR, Role.DISPATCHER})
    @GetMapping("/dispatcher/{dispatcherUid}")
    public ResponseList<DispatcherShift> getShiftsByDispatcher(@PathVariable String dispatcherUid) {
        return dispatcherShiftService.getShiftsByDispatcher(dispatcherUid);
    }

    @Authenticated
    @AuthorizedRole({Role.ROOT, Role.AGENCY_ADMIN, Role.DISPATCH_CENTER_ADMIN, Role.DISPATCHER_SUPERVISOR, Role.DISPATCHER})
    @GetMapping("/on-duty")
    public ResponseList<DispatcherShift> getOnDutyNow() {
        return dispatcherShiftService.getOnDutyNow();
    }
}
