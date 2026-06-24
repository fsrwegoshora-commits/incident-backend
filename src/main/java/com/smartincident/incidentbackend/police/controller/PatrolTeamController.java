package com.smartincident.incidentbackend.police.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.RequiresPermission;
import com.smartincident.incidentbackend.enums.PatrolTeamStatus;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.police.dto.PatrolTeamDto;
import com.smartincident.incidentbackend.police.entity.PatrolTeam;
import com.smartincident.incidentbackend.police.service.PatrolTeamService;
import com.smartincident.incidentbackend.utils.PageableParam;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponsePage;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/police/patrol-teams")
@RequiredArgsConstructor
public class PatrolTeamController {

    private final PatrolTeamService patrolTeamService;

    @Authenticated
    @RequiresPermission(Permission.MANAGE_STATIONS)
    @PostMapping
    public Response<PatrolTeam> savePatrolTeam(@RequestBody PatrolTeamDto dto) {
        return patrolTeamService.savePatrolTeam(dto);
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_RESPONDERS)
    @GetMapping
    public ResponsePage<PatrolTeam> getPatrolTeams(@ModelAttribute PageableParam pageableParam) {
        return patrolTeamService.getPatrolTeams(pageableParam != null ? pageableParam : new PageableParam());
    }

    @Authenticated
    @RequiresPermission(Permission.VIEW_RESPONDERS)
    @GetMapping("/{uid}")
    public Response<PatrolTeam> getPatrolTeam(@PathVariable String uid) {
        return patrolTeamService.getPatrolTeam(uid);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_STATIONS)
    @PatchMapping("/{uid}/status")
    public Response<PatrolTeam> updateStatus(
            @PathVariable String uid,
            @RequestParam PatrolTeamStatus status) {
        return patrolTeamService.updateStatus(uid, status);
    }

    @Authenticated
    @RequiresPermission(Permission.MANAGE_STATIONS)
    @DeleteMapping("/{uid}")
    public Response<PatrolTeam> deletePatrolTeam(@PathVariable String uid) {
        return patrolTeamService.deletePatrolTeam(uid);
    }
}
