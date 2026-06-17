package com.smartincident.incidentbackend.shift.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.shift.dto.OnDutyPersonnelDto;
import com.smartincident.incidentbackend.shift.service.ShiftService;
import com.smartincident.incidentbackend.utils.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
@Slf4j
public class ShiftController {

    private final ShiftService shiftService;

    /** Unified view of all on-duty personnel across police, fire, medical and dispatch. */
    @Authenticated
    @AuthorizedRole({Role.DISPATCHER, Role.DISPATCHER_SUPERVISOR, Role.DISPATCH_CENTER_ADMIN,
                     Role.STATION_ADMIN, Role.AGENCY_ADMIN, Role.ROOT})
    @GetMapping("/on-duty")
    public Response<OnDutyPersonnelDto> getOnDutyPersonnel() {
        log.info("Fetching all on-duty personnel");
        return shiftService.getOnDutyPersonnel();
    }
}
