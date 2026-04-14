package com.smartincident.incidentbackend.emergency.service;

import com.smartincident.incidentbackend.emergency.dto.VehicleShiftDto;
import com.smartincident.incidentbackend.emergency.entity.EmergencyVehicle;
import com.smartincident.incidentbackend.emergency.entity.VehicleShift;
import com.smartincident.incidentbackend.emergency.repository.EmergencyVehicleRepository;
import com.smartincident.incidentbackend.emergency.repository.VehicleShiftRepository;
import com.smartincident.incidentbackend.police.entity.PoliceOfficer;
import com.smartincident.incidentbackend.police.entity.TrafficCheckpoint;
import com.smartincident.incidentbackend.police.repository.PoliceOfficerRepository;
import com.smartincident.incidentbackend.police.repository.TrafficCheckPointRepository;
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
public class VehicleShiftService {

    private final VehicleShiftRepository shiftRepository;
    private final EmergencyVehicleRepository vehicleRepository;
    private final PoliceOfficerRepository officerRepository;
    private final TrafficCheckPointRepository checkpointRepository;

    @Transactional
    public Response<VehicleShift> saveShift(VehicleShiftDto dto) {
        if (dto == null) return Response.error("Shift DTO cannot be null");

        VehicleShift shift;

        if (dto.getUid() != null) {
            // ── UPDATE ──────────────────────────────────────────────────────
            Optional<VehicleShift> existing = shiftRepository.findByUid(dto.getUid());
            if (existing.isEmpty()) return Response.error("Shift not found");
            shift = existing.get();

            if (dto.getShiftDate() != null) shift.setShiftDate(dto.getShiftDate());
            if (dto.getShiftTime() != null) shift.setShiftTime(dto.getShiftTime());
            if (dto.getStartTime() != null) shift.setStartTime(dto.getStartTime());
            if (dto.getEndTime() != null) shift.setEndTime(dto.getEndTime());
            if (dto.getDutyDescription() != null) shift.setDutyDescription(dto.getDutyDescription());

            if (dto.getCrewUids() != null) {
                List<PoliceOfficer> crew = resolveOfficers(dto.getCrewUids());
                shift.setCrew(crew);
            }
            if (dto.getStandbyLocationUid() != null) {
                Optional<TrafficCheckpoint> cp = checkpointRepository.findByUid(dto.getStandbyLocationUid());
                cp.ifPresent(shift::setStandbyLocation);
            }
            shift.update();

        } else {
            // ── CREATE ──────────────────────────────────────────────────────
            if (dto.getVehicleUid() == null) return Response.error("Vehicle is required");
            if (dto.getShiftDate() == null) return Response.error("Shift date is required");
            if (dto.getShiftTime() == null) return Response.error("Shift time is required");
            if (dto.getStartTime() == null) return Response.error("Start time is required");
            if (dto.getEndTime() == null) return Response.error("End time is required");

            Optional<EmergencyVehicle> vehicleOpt = vehicleRepository.findByUid(dto.getVehicleUid());
            if (vehicleOpt.isEmpty()) return Response.error("Vehicle not found");

            List<PoliceOfficer> crew = new ArrayList<>();
            if (dto.getCrewUids() != null && !dto.getCrewUids().isEmpty()) {
                crew = resolveOfficers(dto.getCrewUids());
            }

            TrafficCheckpoint standby = null;
            if (dto.getStandbyLocationUid() != null) {
                standby = checkpointRepository.findByUid(dto.getStandbyLocationUid()).orElse(null);
            }

            shift = VehicleShift.builder()
                    .vehicle(vehicleOpt.get())
                    .shiftDate(dto.getShiftDate())
                    .shiftTime(dto.getShiftTime())
                    .startTime(dto.getStartTime())
                    .endTime(dto.getEndTime())
                    .dutyDescription(dto.getDutyDescription())
                    .crew(crew)
                    .standbyLocation(standby)
                    .build();
        }

        try {
            VehicleShift saved = shiftRepository.save(shift);
            log.info("Saved vehicle shift for vehicle: {} on {}", saved.getVehicle().getPlateNumber(), saved.getShiftDate());
            return Response.success(saved);
        } catch (Exception e) {
            log.error("Failed to save vehicle shift: {}", e.getMessage());
            return Response.error("Failed to save vehicle shift: " + Utils.getExceptionMessage(e));
        }
    }

    public Response<VehicleShift> deleteShift(String uid) {
        if (uid == null) return Response.error("UID is required");
        Optional<VehicleShift> opt = shiftRepository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Shift not found");
        VehicleShift s = opt.get();
        s.delete();
        shiftRepository.save(s);
        return Response.success(s);
    }

    public ResponsePage<VehicleShift> getShiftsByStation(PageableParam param, String stationUid) {
        return new ResponsePage<>(shiftRepository.findByStationUid(
                param.getPageable(false), param.getIsActive(), stationUid));
    }

    public ResponsePage<VehicleShift> getShiftsByVehicle(PageableParam param, String vehicleUid) {
        return new ResponsePage<>(shiftRepository.findByVehicleUid(
                param.getPageable(false), param.getIsActive(), vehicleUid));
    }

    private List<PoliceOfficer> resolveOfficers(List<String> uids) {
        List<PoliceOfficer> result = new ArrayList<>();
        for (String uid : uids) {
            officerRepository.findByUid(uid).ifPresent(result::add);
        }
        return result;
    }
}
