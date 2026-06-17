package com.smartincident.incidentbackend.setting.controller;

import com.smartincident.incidentbackend.authotp.dto.UserDto;
import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.authotp.service.UserService;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.setting.dto.AgencyDto;
import com.smartincident.incidentbackend.setting.dto.OfficerSummaryDto;
import com.smartincident.incidentbackend.setting.entity.Agency;
import com.smartincident.incidentbackend.setting.service.AgencyService;
import com.smartincident.incidentbackend.utils.PageableParam;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponsePage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/agencies")
@RequiredArgsConstructor
public class AgencyController {

    private final AgencyService agencyService;
    private final UserService userService;

    @Authenticated
    @RequiresPermission(Permission.MANAGE_AGENCY)
    @PostMapping
    public Response<Agency> saveAgency(@RequestBody AgencyDto agencyDto) {
        return agencyService.saveAgency(agencyDto);
    }

    @Authenticated
    @GetMapping("/{uid}")
    public Response<Agency> getAgency(@PathVariable String uid) {
        return agencyService.getAgencyByUid(uid);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_AGENCY)
    @DeleteMapping("/{uid}")
    public Response<Agency> deleteAgency(@PathVariable String uid) {
        return agencyService.deleteAgency(uid);
    }

    @Authenticated
    @GetMapping
    public ResponsePage<Agency> getAgencies(@ModelAttribute PageableParam pageableParam) {
        return agencyService.getAgencies(pageableParam != null ? pageableParam : new PageableParam());
    }

    @Authenticated
    @GetMapping("/{agencyUid}/admins")
    public Response<List<User>> getAgencyAdmins(@PathVariable String agencyUid) {
        return agencyService.getAgencyAdmins(agencyUid);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_AGENCY)
    @PostMapping("/{agencyUid}/admins")
    public Response<User> registerAgencyAdmin(@PathVariable String agencyUid, @RequestBody UserDto userDto) {
        return userService.registerAgencyAdmin(agencyUid, userDto);
    }

    @Authenticated
    @GetMapping("/{agencyUid}/available-officers")
    public Response<List<OfficerSummaryDto>> getAvailableOfficers(@PathVariable String agencyUid) {
        return agencyService.getAvailableOfficers(agencyUid);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_AGENCY)
    @PostMapping("/{agencyUid}/admins/assign/{userUid}")
    public Response<User> assignOfficerAsAdmin(@PathVariable String agencyUid, @PathVariable String userUid) {
        return agencyService.assignOfficerAsAdmin(agencyUid, userUid);
    }
}
