package com.smartincident.incidentbackend.police.service;

import com.smartincident.incidentbackend.enums.PatrolTeamStatus;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.police.dto.PatrolTeamDto;
import com.smartincident.incidentbackend.police.entity.*;
import com.smartincident.incidentbackend.police.repository.*;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatrolTeamService {

    private final PatrolTeamRepository patrolTeamRepository;
    private final PoliceStationRepository stationRepository;
    private final PoliceVehicleRepository vehicleRepository;
    private final PoliceOfficerRepository officerRepository;

    @Transactional
    public Response<PatrolTeam> savePatrolTeam(PatrolTeamDto dto) {
        if (dto == null)           return Response.error("Patrol team data is required");
        if (dto.getName() == null || dto.getName().isBlank())
                                   return Response.error("Team name is required");

        PatrolTeam team;
        if (dto.getUid() != null) {
            Optional<PatrolTeam> opt = patrolTeamRepository.findByUid(dto.getUid());
            if (opt.isEmpty())     return Response.error("Patrol team not found");
            team = opt.get();
            team.update();
        } else {
            team = new PatrolTeam();
        }

        team.setName(dto.getName().trim());

        // Station — required on create, optional on update
        if (dto.getStationUid() != null) {
            PoliceStation station = stationRepository.findByUid(dto.getStationUid()).orElse(null);
            if (station == null)   return Response.error("Police station not found");
            team.setStation(station);
        } else if (dto.getUid() == null) {
            String stationUid = LoggedUser.getPoliceStationUid();
            if (stationUid == null) return Response.error("Station is required");
            PoliceStation station = stationRepository.findByUid(stationUid).orElse(null);
            if (station == null)   return Response.error("Station not found for logged-in user");
            team.setStation(station);
        }

        // Vehicle (optional)
        if (dto.getVehicleUid() != null && !dto.getVehicleUid().isBlank()) {
            PoliceVehicle v = vehicleRepository.findByUid(dto.getVehicleUid()).orElse(null);
            if (v == null)         return Response.error("Vehicle not found");
            team.setVehicle(v);
        } else if (dto.getVehicleUid() != null) {
            team.setVehicle(null);
        }

        // Team Leader (optional)
        if (dto.getTeamLeaderUid() != null && !dto.getTeamLeaderUid().isBlank()) {
            PoliceOfficer leader = officerRepository.findByUid(dto.getTeamLeaderUid()).orElse(null);
            if (leader == null)    return Response.error("Team leader officer not found");
            team.setTeamLeader(leader);
        } else if (dto.getTeamLeaderUid() != null) {
            team.setTeamLeader(null);
        }

        // Driver (optional)
        if (dto.getDriverUid() != null && !dto.getDriverUid().isBlank()) {
            PoliceOfficer driver = officerRepository.findByUid(dto.getDriverUid()).orElse(null);
            if (driver == null)    return Response.error("Driver officer not found");
            team.setDriver(driver);
        } else if (dto.getDriverUid() != null) {
            team.setDriver(null);
        }

        // Members (optional)
        if (dto.getMemberUids() != null) {
            List<PoliceOfficer> members = new ArrayList<>();
            for (String uid : dto.getMemberUids()) {
                PoliceOfficer o = officerRepository.findByUid(uid).orElse(null);
                if (o == null)     return Response.error("Officer not found: " + uid);
                members.add(o);
            }
            team.setMembers(members);
        }

        if (dto.getCoverageArea() != null) team.setCoverageArea(dto.getCoverageArea().trim());
        if (dto.getNotes()        != null) team.setNotes(dto.getNotes().trim());
        if (dto.getStatus()       != null) team.setStatus(dto.getStatus());
        else if (dto.getUid()     == null) team.setStatus(PatrolTeamStatus.STANDBY);

        try {
            PatrolTeam saved = patrolTeamRepository.save(team);
            log.info("Patrol team '{}' saved by {}", saved.getName(), LoggedUser.getName());
            return Response.success(saved);
        } catch (Exception e) {
            log.error("Failed to save patrol team: {}", e.getMessage());
            return Response.error("Failed to save patrol team: " + Utils.getExceptionMessage(e));
        }
    }

    public Response<PatrolTeam> getPatrolTeam(String uid) {
        return patrolTeamRepository.findByUid(uid)
                .map(Response::new)
                .orElseGet(() -> Response.error("Patrol team not found"));
    }

    public ResponsePage<PatrolTeam> getPatrolTeams(PageableParam pageableParam) {
        Role role = LoggedUser.getRole();
        if (role == Role.AGENCY_ADMIN) {
            String agencyUid = LoggedUser.getAgencyUid();
            if (agencyUid != null) {
                return new ResponsePage<>(patrolTeamRepository.findByAgency(
                        pageableParam.getPageable(true), pageableParam.getIsActive(),
                        pageableParam.key(), agencyUid));
            }
        }
        String stationUid = LoggedUser.getPoliceStationUid();
        if (stationUid == null) return new ResponsePage<>("Station not found for logged-in user");
        return new ResponsePage<>(patrolTeamRepository.findByStation(
                pageableParam.getPageable(true), pageableParam.getIsActive(),
                pageableParam.key(), stationUid));
    }

    @Transactional
    public Response<PatrolTeam> updateStatus(String uid, PatrolTeamStatus status) {
        Optional<PatrolTeam> opt = patrolTeamRepository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Patrol team not found");
        PatrolTeam team = opt.get();
        team.setStatus(status);
        team.update();
        patrolTeamRepository.save(team);
        log.info("Patrol team '{}' status changed to {}", team.getName(), status);
        return Response.success(team);
    }

    @Transactional
    public Response<PatrolTeam> deletePatrolTeam(String uid) {
        Optional<PatrolTeam> opt = patrolTeamRepository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Patrol team not found");
        PatrolTeam team = opt.get();
        team.delete();
        patrolTeamRepository.save(team);
        log.info("Patrol team '{}' deleted", team.getName());
        return Response.success(team);
    }
}
