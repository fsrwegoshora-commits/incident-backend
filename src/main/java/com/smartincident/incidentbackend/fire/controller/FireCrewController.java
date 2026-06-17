package com.smartincident.incidentbackend.fire.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.enums.FireCrewStatus;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.fire.dto.FireCrewDto;
import com.smartincident.incidentbackend.fire.entity.FireCrew;
import com.smartincident.incidentbackend.fire.service.FireCrewService;
import com.smartincident.incidentbackend.utils.PageableParam;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponsePage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fire/crews")
@RequiredArgsConstructor
public class FireCrewController {

    private final FireCrewService crewService;

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.AGENCY_ADMIN, Role.ROOT})
    @PostMapping
    public Response<FireCrew> saveCrew(@RequestBody FireCrewDto dto) {
        return crewService.saveCrew(dto);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.AGENCY_ADMIN, Role.ROOT,
                     Role.DISPATCHER, Role.DISPATCHER_SUPERVISOR, Role.DISPATCH_CENTER_ADMIN})
    @GetMapping
    public ResponsePage<FireCrew> getCrews(
            @ModelAttribute PageableParam pageableParam,
            @RequestParam String unitUid) {
        return crewService.getCrews(
                pageableParam != null ? pageableParam : new PageableParam(), unitUid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.AGENCY_ADMIN, Role.ROOT,
                     Role.DISPATCHER, Role.DISPATCHER_SUPERVISOR, Role.DISPATCH_CENTER_ADMIN})
    @GetMapping("/{uid}")
    public Response<FireCrew> getCrew(@PathVariable String uid) {
        return crewService.getCrew(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.AGENCY_ADMIN, Role.ROOT})
    @PatchMapping("/{uid}/status")
    public Response<FireCrew> updateStatus(
            @PathVariable String uid,
            @RequestParam FireCrewStatus status) {
        return crewService.updateStatus(uid, status);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.AGENCY_ADMIN, Role.ROOT})
    @DeleteMapping("/{uid}")
    public Response<FireCrew> deleteCrew(@PathVariable String uid) {
        return crewService.deleteCrew(uid);
    }
}
