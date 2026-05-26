package com.smartincident.incidentbackend.emergency.service;

import com.smartincident.incidentbackend.emergency.dto.VehicleShiftDto;
import com.smartincident.incidentbackend.emergency.entity.EmergencyVehicle;
import com.smartincident.incidentbackend.emergency.entity.VehicleShift;
import com.smartincident.incidentbackend.emergency.repository.EmergencyVehicleRepository;
import com.smartincident.incidentbackend.emergency.repository.VehicleShiftRepository;
import com.smartincident.incidentbackend.medical.entity.Hospital;
import com.smartincident.incidentbackend.medical.entity.Medic;
import com.smartincident.incidentbackend.medical.repository.HospitalRepository;
import com.smartincident.incidentbackend.medical.repository.MedicRepository;
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
    private final MedicRepository medicRepository;
    private final HospitalRepository hospitalRepository;

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

            // ── Police / Fire crew ──
            if (dto.getCrewUids() != null) {
                shift.setCrew(resolveOfficers(dto.getCrewUids()));
            }
            if (dto.getStandbyLocationUid() != null) {
                checkpointRepository.findByUid(dto.getStandbyLocationUid())
                        .ifPresent(shift::setStandbyLocation);
            }

            // ── Ambulance crew ──
            if (dto.getMedicUids() != null) {
                shift.setMedics(resolveMedics(dto.getMedicUids()));
            }
            if (dto.getInChargeUid() != null) {
                medicRepository.findByUid(dto.getInChargeUid()).ifPresent(shift::setInCharge);
            }
            if (dto.getDriverUid() != null) {
                medicRepository.findByUid(dto.getDriverUid()).ifPresent(shift::setDriver);
            }
            if (dto.getStandbyHospitalUid() != null) {
                hospitalRepository.findByUid(dto.getStandbyHospitalUid()).ifPresent(shift::setStandbyHospital);
            }
            if (dto.getStandbyLocationName() != null) {
                shift.setStandbyLocationName(dto.getStandbyLocationName());
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

            List<Medic> medics = new ArrayList<>();
            if (dto.getMedicUids() != null && !dto.getMedicUids().isEmpty()) {
                medics = resolveMedics(dto.getMedicUids());
            }

            Medic inCharge = null;
            if (dto.getInChargeUid() != null) {
                inCharge = medicRepository.findByUid(dto.getInChargeUid()).orElse(null);
            }

            Medic driver = null;
            if (dto.getDriverUid() != null) {
                driver = medicRepository.findByUid(dto.getDriverUid()).orElse(null);
            }

            Hospital standbyHospital = null;
            if (dto.getStandbyHospitalUid() != null) {
                standbyHospital = hospitalRepository.findByUid(dto.getStandbyHospitalUid()).orElse(null);
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
                    .medics(medics)
                    .inCharge(inCharge)
                    .driver(driver)
                    .standbyHospital(standbyHospital)
                    .standbyLocationName(dto.getStandbyLocationName())
                    .build();
        }

        try {
            VehicleShift saved = shiftRepository.save(shift);
            log.info("Saved vehicle shift for vehicle: {} on {}",
                    saved.getVehicle().getPlateNumber(), saved.getShiftDate());
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

    private List<Medic> resolveMedics(List<String> uids) {
        if (uids == null || uids.isEmpty()) return new ArrayList<>();
        return medicRepository.findByUidIn(uids);
    }
}
