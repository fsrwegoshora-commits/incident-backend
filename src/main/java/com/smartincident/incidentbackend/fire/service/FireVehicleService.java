package com.smartincident.incidentbackend.fire.service;

import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.emergency.repository.EmergencyUnitRepository;
import com.smartincident.incidentbackend.enums.VehicleStatus;
import com.smartincident.incidentbackend.enums.VehicleType;
import com.smartincident.incidentbackend.fire.dto.FireVehicleDto;
import com.smartincident.incidentbackend.fire.entity.FireVehicle;
import com.smartincident.incidentbackend.fire.repository.FireVehicleRepository;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FireVehicleService {

    private final FireVehicleRepository vehicleRepository;
    private final EmergencyUnitRepository unitRepository;

    @Transactional
    public Response<FireVehicle> saveVehicle(FireVehicleDto dto) {
        if (dto == null) return Response.error("Vehicle data is required");

        FireVehicle vehicle;

        if (dto.getUid() != null) {
            // ── UPDATE ──────────────────────────────────────────────────────
            vehicle = vehicleRepository.findByUid(dto.getUid())
                    .orElse(null);
            if (vehicle == null) return Response.error("Fire vehicle not found");

            if (dto.getPlateNumber() != null) vehicle.setPlateNumber(dto.getPlateNumber().toUpperCase().trim());
            if (dto.getModel() != null) vehicle.setModel(dto.getModel().trim());
            if (dto.getStatus() != null) vehicle.setStatus(dto.getStatus());
            if (dto.getEquipmentList() != null) vehicle.setEquipmentList(dto.getEquipmentList());

            VehicleType effectiveType = dto.getVehicleType() != null ? dto.getVehicleType() : vehicle.getVehicleType();
            if (dto.getVehicleType() != null) {
                if (!FireVehicle.ALLOWED_TYPES.contains(dto.getVehicleType()))
                    return Response.error("Invalid fire vehicle type: " + dto.getVehicleType());
                vehicle.setVehicleType(dto.getVehicleType());
            }

            if (FireVehicle.WATER_CARRYING_TYPES.contains(effectiveType)) {
                if (dto.getWaterCapacityLitres() != null) vehicle.setWaterCapacityLitres(dto.getWaterCapacityLitres());
            } else {
                vehicle.setWaterCapacityLitres(null);
            }

            if (dto.getEmergencyUnitUid() != null) {
                EmergencyUnit unit = unitRepository.findByUid(dto.getEmergencyUnitUid()).orElse(null);
                if (unit == null) return Response.error("Emergency unit not found");
                vehicle.setEmergencyUnit(unit);
            }
            vehicle.update();

        } else {
            // ── CREATE ──────────────────────────────────────────────────────
            if (dto.getPlateNumber() == null || dto.getPlateNumber().isBlank())
                return Response.error("Plate number is required");
            if (dto.getModel() == null || dto.getModel().isBlank())
                return Response.error("Vehicle model is required");
            if (dto.getVehicleType() == null)
                return Response.error("Vehicle type is required");
            if (!FireVehicle.ALLOWED_TYPES.contains(dto.getVehicleType()))
                return Response.error("Invalid fire vehicle type: " + dto.getVehicleType());
            if (dto.getEmergencyUnitUid() == null)
                return Response.error("Emergency unit is required");
            if (vehicleRepository.existsByPlateNumber(dto.getPlateNumber().toUpperCase().trim()))
                return Response.error("A vehicle with plate '" + dto.getPlateNumber() + "' already exists");

            EmergencyUnit unit = unitRepository.findByUid(dto.getEmergencyUnitUid()).orElse(null);
            if (unit == null) return Response.error("Emergency unit not found");

            boolean isWaterCarrying = FireVehicle.WATER_CARRYING_TYPES.contains(dto.getVehicleType());

            vehicle = FireVehicle.builder()
                    .waterCapacityLitres(isWaterCarrying ? dto.getWaterCapacityLitres() : null)
                    .equipmentList(dto.getEquipmentList())
                    .build();
            vehicle.setPlateNumber(dto.getPlateNumber().toUpperCase().trim());
            vehicle.setModel(dto.getModel().trim());
            vehicle.setVehicleType(dto.getVehicleType());
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            vehicle.setEmergencyUnit(unit);
        }

        try {
            FireVehicle saved = vehicleRepository.save(vehicle);
            log.info("Saved fire vehicle: {} ({})", saved.getPlateNumber(), saved.getVehicleType());
            return Response.success(saved);
        } catch (Exception e) {
            log.error("Failed to save fire vehicle: {}", e.getMessage());
            return Response.error("Failed to save vehicle: " + Utils.getExceptionMessage(e));
        }
    }

    public Response<FireVehicle> getVehicle(String uid) {
        if (uid == null) return Response.error("UID is required");
        return vehicleRepository.findByUid(uid)
                .map(Response::new)
                .orElseGet(() -> Response.error("Fire vehicle not found"));
    }

    @Transactional
    public Response<FireVehicle> deleteVehicle(String uid) {
        if (uid == null) return Response.error("UID is required");
        FireVehicle v = vehicleRepository.findByUid(uid).orElse(null);
        if (v == null) return Response.error("Fire vehicle not found");
        if (!v.getIsActive()) return Response.error("Vehicle already deleted");
        v.delete();
        vehicleRepository.save(v);
        return Response.success(v);
    }

    public ResponsePage<FireVehicle> getVehicles(PageableParam param) {
        String unitUid = LoggedUser.getEmergencyUnitUid();
        return new ResponsePage<>(vehicleRepository.findFireVehicles(
                param.getPageable(false), param.getIsActive(), param.key(), unitUid));
    }

    public ResponsePage<FireVehicle> getVehiclesByUnit(PageableParam param, String unitUid) {
        return new ResponsePage<>(vehicleRepository.findFireVehicles(
                param.getPageable(false), param.getIsActive(), param.key(), unitUid));
    }

    public ResponseList<FireVehicle> getAvailableVehicles() {
        String unitUid = LoggedUser.getEmergencyUnitUid();
        if (unitUid == null) return ResponseList.error("Emergency unit context missing");
        return new ResponseList<>(vehicleRepository.findAvailableByUnit(unitUid));
    }

    @Transactional
    public Response<FireVehicle> updateStatus(String uid, String statusStr) {
        if (uid == null) return Response.error("UID is required");
        VehicleStatus status;
        try { status = VehicleStatus.valueOf(statusStr.toUpperCase()); }
        catch (IllegalArgumentException e) { return Response.error("Invalid status: " + statusStr); }
        FireVehicle v = vehicleRepository.findByUid(uid).orElse(null);
        if (v == null) return Response.error("Fire vehicle not found");
        v.setStatus(status);
        v.update();
        vehicleRepository.save(v);
        return Response.success(v);
    }
}
