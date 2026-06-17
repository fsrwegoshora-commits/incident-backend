package com.smartincident.incidentbackend.fire.service;

import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.emergency.repository.EmergencyUnitRepository;
import com.smartincident.incidentbackend.enums.FireCrewStatus;
import com.smartincident.incidentbackend.fire.dto.FireCrewDto;
import com.smartincident.incidentbackend.fire.entity.FireCrew;
import com.smartincident.incidentbackend.fire.entity.FireOfficer;
import com.smartincident.incidentbackend.fire.entity.FireVehicle;
import com.smartincident.incidentbackend.fire.repository.FireCrewRepository;
import com.smartincident.incidentbackend.fire.repository.FireOfficerRepository;
import com.smartincident.incidentbackend.fire.repository.FireVehicleRepository;
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
public class FireCrewService {

    private final FireCrewRepository crewRepository;
    private final EmergencyUnitRepository unitRepository;
    private final FireOfficerRepository officerRepository;
    private final FireVehicleRepository vehicleRepository;

    @Transactional
    public Response<FireCrew> saveCrew(FireCrewDto dto) {
        if (dto == null || dto.getName() == null || dto.getName().isBlank())
            return Response.error("Crew name is required");

        FireCrew crew;
        if (dto.getUid() != null) {
            Optional<FireCrew> opt = crewRepository.findByUid(dto.getUid());
            if (opt.isEmpty()) return Response.error("Fire crew not found");
            crew = opt.get();
            crew.update();
        } else {
            crew = new FireCrew();
        }

        crew.setName(dto.getName().trim());

        // Unit — required on create
        if (dto.getUnitUid() != null) {
            EmergencyUnit unit = unitRepository.findByUid(dto.getUnitUid()).orElse(null);
            if (unit == null) return Response.error("Fire unit not found");
            crew.setUnit(unit);
        } else if (dto.getUid() == null) {
            return Response.error("Fire unit is required");
        }

        // Vehicle (optional)
        if (dto.getVehicleUid() != null && !dto.getVehicleUid().isBlank()) {
            FireVehicle v = vehicleRepository.findByUid(dto.getVehicleUid()).orElse(null);
            if (v == null) return Response.error("Fire vehicle not found");
            crew.setVehicle(v);
        } else if (dto.getVehicleUid() != null) {
            crew.setVehicle(null);
        }

        // Crew Commander (optional)
        if (dto.getCrewCommanderUid() != null && !dto.getCrewCommanderUid().isBlank()) {
            FireOfficer commander = officerRepository.findByUid(dto.getCrewCommanderUid()).orElse(null);
            if (commander == null) return Response.error("Crew commander not found");
            crew.setCrewCommander(commander);
        } else if (dto.getCrewCommanderUid() != null) {
            crew.setCrewCommander(null);
        }

        // Driver (optional)
        if (dto.getDriverUid() != null && !dto.getDriverUid().isBlank()) {
            FireOfficer driver = officerRepository.findByUid(dto.getDriverUid()).orElse(null);
            if (driver == null) return Response.error("Driver not found");
            crew.setDriver(driver);
        } else if (dto.getDriverUid() != null) {
            crew.setDriver(null);
        }

        // Members (optional)
        if (dto.getMemberUids() != null) {
            List<FireOfficer> members = new ArrayList<>();
            for (String uid : dto.getMemberUids()) {
                FireOfficer o = officerRepository.findByUid(uid).orElse(null);
                if (o == null) return Response.error("Fire officer not found: " + uid);
                members.add(o);
            }
            crew.setMembers(members);
        }

        if (dto.getCoverageArea() != null) crew.setCoverageArea(dto.getCoverageArea().trim());
        if (dto.getNotes()        != null) crew.setNotes(dto.getNotes().trim());
        if (dto.getStatus()       != null) crew.setStatus(dto.getStatus());
        else if (dto.getUid()     == null) crew.setStatus(FireCrewStatus.STANDBY);

        try {
            FireCrew saved = crewRepository.save(crew);
            log.info("Fire crew '{}' saved", saved.getName());
            return Response.success(saved);
        } catch (Exception e) {
            return Response.error("Failed to save fire crew: " + Utils.getExceptionMessage(e));
        }
    }

    public ResponsePage<FireCrew> getCrews(PageableParam pageableParam, String unitUid) {
        if (unitUid == null || unitUid.isBlank())
            return new ResponsePage<>("unitUid is required");
        return new ResponsePage<>(crewRepository.findByUnit(
                pageableParam.getPageable(true), pageableParam.getIsActive(),
                pageableParam.key(), unitUid));
    }

    public Response<FireCrew> getCrew(String uid) {
        return crewRepository.findByUid(uid)
                .map(Response::new)
                .orElseGet(() -> Response.error("Fire crew not found"));
    }

    @Transactional
    public Response<FireCrew> updateStatus(String uid, FireCrewStatus status) {
        Optional<FireCrew> opt = crewRepository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Fire crew not found");
        opt.get().setStatus(status);
        opt.get().update();
        crewRepository.save(opt.get());
        return Response.success(opt.get());
    }

    @Transactional
    public Response<FireCrew> deleteCrew(String uid) {
        Optional<FireCrew> opt = crewRepository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Fire crew not found");
        opt.get().delete();
        crewRepository.save(opt.get());
        return Response.success(opt.get());
    }
}
