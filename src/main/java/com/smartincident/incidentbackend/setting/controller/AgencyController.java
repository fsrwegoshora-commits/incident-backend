package com.smartincident.incidentbackend.setting.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.setting.dto.AgencyDto;
import com.smartincident.incidentbackend.setting.entity.Agency;
import com.smartincident.incidentbackend.setting.service.AgencyService;
import com.smartincident.incidentbackend.utils.PageableParam;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponsePage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/api/agencies")
@RequiredArgsConstructor
public class AgencyController {
    private final AgencyService agencyService;

    @Authenticated
    @AuthorizedRole({Role.ROOT})
    @PostMapping
    public Response<Agency> saveAgency(@RequestBody AgencyDto agencyDto) {
        return agencyService.saveAgency(agencyDto);
    }

    @Authenticated
    @AuthorizedRole({Role.ROOT, Role.STATION_ADMIN})
    @GetMapping("/{uid}")
    public Response<Agency> getAgency(@PathVariable String uid) {
        return agencyService.getAgencyByUid(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.ROOT})
    @DeleteMapping("/{uid}")
    public Response<Agency> deleteAgency(@PathVariable String uid) {
        return agencyService.deleteAgency(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.ROOT, Role.STATION_ADMIN})
    @GetMapping
    public ResponsePage<Agency> getAgencies(@ModelAttribute PageableParam pageableParam) {
        return agencyService.getAgencies(pageableParam != null ? pageableParam : new PageableParam());
    }
}
