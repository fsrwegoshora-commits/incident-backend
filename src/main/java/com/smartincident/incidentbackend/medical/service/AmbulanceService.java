package com.smartincident.incidentbackend.medical.service;

import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.emergency.repository.EmergencyUnitRepository;
import com.smartincident.incidentbackend.enums.VehicleStatus;
import com.smartincident.incidentbackend.medical.dto.AmbulanceDto;
import com.smartincident.incidentbackend.medical.entity.Ambulance;
import com.smartincident.incidentbackend.medical.entity.Hospital;
import com.smartincident.incidentbackend.medical.repository.AmbulanceRepository;
import com.smartincident.incidentbackend.medical.repository.HospitalRepository;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmbulanceService {

    private final AmbulanceRepository ambulanceRepository;
    private final EmergencyUnitRepository unitRepository;
    private final HospitalRepository hospitalRepository;

    @Transactional
    public Response<Ambulance> saveAmbulance(AmbulanceDto dto) {
        if (dto == null) return Response.error("Ambulance data is required");

        Ambulance ambulance;

        if (dto.getUid() != null) {
            // ── UPDATE ──────────────────────────────────────────────────────
            ambulance = ambulanceRepository.findByUid(dto.getUid()).orElse(null);
            if (ambulance == null) return Response.error("Ambulance not found");

            if (dto.getPlateNumber() != null) ambulance.setPlateNumber(dto.getPlateNumber().toUpperCase().trim());
            if (dto.getModel() != null) ambulance.setModel(dto.getModel().trim());
            if (dto.getStatus() != null) ambulance.setStatus(dto.getStatus());
            if (dto.getVehicleType() != null) {
                if (!Ambulance.ALLOWED_TYPES.contains(dto.getVehicleType()))
                    return Response.error("Invalid ambulance type: " + dto.getVehicleType());
                ambulance.setVehicleType(dto.getVehicleType());
            }
            if (dto.getHasAdvancedLifeSupport() != null) ambulance.setHasAdvancedLifeSupport(dto.getHasAdvancedLifeSupport());
            if (dto.getStretcherCapacity() != null) ambulance.setStretcherCapacity(dto.getStretcherCapacity());

            if (dto.getBaseHospitalUid() != null) {
                Hospital hospital = hospitalRepository.findByUid(dto.getBaseHospitalUid()).orElse(null);
                if (hospital == null) return Response.error("Hospital not found");
                ambulance.setBaseHospital(hospital);
            }
            if (dto.getEmergencyUnitUid() != null) {
                EmergencyUnit unit = unitRepository.findByUid(dto.getEmergencyUnitUid()).orElse(null);
                if (unit == null) return Response.error("Emergency unit not found");
                ambulance.setEmergencyUnit(unit);
            }
            ambulance.update();

        } else {
            // ── CREATE ──────────────────────────────────────────────────────
            if (dto.getPlateNumber() == null || dto.getPlateNumber().isBlank())
                return Response.error("Plate number is required");
            if (dto.getModel() == null || dto.getModel().isBlank())
                return Response.error("Ambulance model is required");
            if (dto.getVehicleType() == null)
                return Response.error("Vehicle type is required");
            if (!Ambulance.ALLOWED_TYPES.contains(dto.getVehicleType()))
                return Response.error("Invalid ambulance type: " + dto.getVehicleType());
            if (dto.getEmergencyUnitUid() == null)
                return Response.error("Emergency unit is required");
            if (dto.getBaseHospitalUid() == null)
                return Response.error("Base hospital is required for ambulances");
            if (ambulanceRepository.existsByPlateNumber(dto.getPlateNumber().toUpperCase().trim()))
                return Response.error("An ambulance with plate '" + dto.getPlateNumber() + "' already exists");

            EmergencyUnit unit = unitRepository.findByUid(dto.getEmergencyUnitUid()).orElse(null);
            if (unit == null) return Response.error("Emergency unit not found");

            Hospital hospital = hospitalRepository.findByUid(dto.getBaseHospitalUid()).orElse(null);
            if (hospital == null) return Response.error("Hospital not found");

            ambulance = Ambulance.builder()
                    .baseHospital(hospital)
                    .build();
            ambulance.setPlateNumber(dto.getPlateNumber().toUpperCase().trim());
            ambulance.setModel(dto.getModel().trim());
            ambulance.setVehicleType(dto.getVehicleType());
            ambulance.setStatus(VehicleStatus.AVAILABLE);
            ambulance.setEmergencyUnit(unit);
            ambulance.setHasAdvancedLifeSupport(Boolean.TRUE.equals(dto.getHasAdvancedLifeSupport()));
            ambulance.setStretcherCapacity(dto.getStretcherCapacity() != null ? dto.getStretcherCapacity() : 1);
        }

        try {
            Ambulance saved = ambulanceRepository.save(ambulance);
            log.info("Saved ambulance: {} ({})", saved.getPlateNumber(), saved.getVehicleType());
            return Response.success(saved);
        } catch (Exception e) {
            log.error("Failed to save ambulance: {}", e.getMessage());
            return Response.error("Failed to save ambulance: " + Utils.getExceptionMessage(e));
        }
    }

    public Response<Ambulance> getAmbulance(String uid) {
        if (uid == null) return Response.error("UID is required");
        return ambulanceRepository.findByUid(uid)
                .map(Response::new)
                .orElseGet(() -> Response.error("Ambulance not found"));
    }

    @Transactional
    public Response<Ambulance> deleteAmbulance(String uid) {
        if (uid == null) return Response.error("UID is required");
        Ambulance a = ambulanceRepository.findByUid(uid).orElse(null);
        if (a == null) return Response.error("Ambulance not found");
        if (!a.getIsActive()) return Response.error("Ambulance already deleted");
        a.delete();
        ambulanceRepository.save(a);
        return Response.success(a);
    }

    public ResponsePage<Ambulance> getAmbulances(PageableParam param) {
        String unitUid = LoggedUser.getEmergencyUnitUid();
        return new ResponsePage<>(ambulanceRepository.findAmbulances(
                param.getPageable(false), param.getIsActive(), param.key(), unitUid));
    }

    public ResponsePage<Ambulance> getAmbulancesByUnit(PageableParam param, String unitUid) {
        return new ResponsePage<>(ambulanceRepository.findAmbulances(
                param.getPageable(false), param.getIsActive(), param.key(), unitUid));
    }

    public ResponseList<Ambulance> getAmbulancesByHospital(String hospitalUid) {
        return new ResponseList<>(ambulanceRepository.findByHospitalUid(hospitalUid));
    }

    public ResponseList<Ambulance> getAvailableAmbulances() {
        String unitUid = LoggedUser.getEmergencyUnitUid();
        if (unitUid == null) return ResponseList.error("Emergency unit context missing");
        return new ResponseList<>(ambulanceRepository.findAvailableByUnit(unitUid));
    }

    @Transactional
    public Response<Ambulance> updateStatus(String uid, String statusStr) {
        if (uid == null) return Response.error("UID is required");
        VehicleStatus status;
        try { status = VehicleStatus.valueOf(statusStr.toUpperCase()); }
        catch (IllegalArgumentException e) { return Response.error("Invalid status: " + statusStr); }
        Ambulance a = ambulanceRepository.findByUid(uid).orElse(null);
        if (a == null) return Response.error("Ambulance not found");
        a.setStatus(status);
        a.update();
        ambulanceRepository.save(a);
        return Response.success(a);
    }
}
