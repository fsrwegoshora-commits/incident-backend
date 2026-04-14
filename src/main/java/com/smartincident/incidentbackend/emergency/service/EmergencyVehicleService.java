package com.smartincident.incidentbackend.emergency.service;

import com.smartincident.incidentbackend.emergency.dto.EmergencyVehicleDto;
import com.smartincident.incidentbackend.emergency.entity.EmergencyVehicle;
import com.smartincident.incidentbackend.emergency.repository.EmergencyVehicleRepository;
import com.smartincident.incidentbackend.enums.VehicleStatus;
import com.smartincident.incidentbackend.enums.VehicleType;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
import com.smartincident.incidentbackend.police.repository.PoliceStationRepository;
import com.smartincident.incidentbackend.setting.entity.Department;
import com.smartincident.incidentbackend.setting.repository.DepartmentRepository;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmergencyVehicleService {

    private final EmergencyVehicleRepository vehicleRepository;
    private final PoliceStationRepository stationRepository;
    private final DepartmentRepository departmentRepository;

    @Transactional
    public Response<EmergencyVehicle> saveVehicle(EmergencyVehicleDto dto) {
        if (dto == null) return Response.error("Vehicle DTO cannot be null");

        EmergencyVehicle vehicle;

        if (dto.getUid() != null) {
            // ── UPDATE ──────────────────────────────────────────────────────
            Optional<EmergencyVehicle> existing = vehicleRepository.findByUid(dto.getUid());
            if (existing.isEmpty()) return Response.error("Vehicle not found");
            vehicle = existing.get();

            if (dto.getPlateNumber() != null) vehicle.setPlateNumber(dto.getPlateNumber());
            if (dto.getModel() != null) vehicle.setModel(dto.getModel());
            if (dto.getVehicleType() != null) vehicle.setVehicleType(dto.getVehicleType());
            if (dto.getStatus() != null) vehicle.setStatus(dto.getStatus());
            if (dto.getWaterCapacityLitres() != null) vehicle.setWaterCapacityLitres(dto.getWaterCapacityLitres());
            if (dto.getEquipmentList() != null) vehicle.setEquipmentList(dto.getEquipmentList());
            if (dto.getHasAdvancedLifeSupport() != null) vehicle.setHasAdvancedLifeSupport(dto.getHasAdvancedLifeSupport());
            if (dto.getStretcherCapacity() != null) vehicle.setStretcherCapacity(dto.getStretcherCapacity());

            if (dto.getStationUid() != null) {
                Optional<PoliceStation> station = stationRepository.findByUid(dto.getStationUid());
                if (station.isEmpty()) return Response.error("Station not found");
                vehicle.setStation(station.get());
            }
            if (dto.getDepartmentUid() != null) {
                Optional<Department> dept = departmentRepository.findByUid(dto.getDepartmentUid());
                if (dept.isEmpty()) return Response.error("Department not found");
                vehicle.setDepartment(dept.get());
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
            if (dto.getStationUid() == null)
                return Response.error("Station is required");
            if (dto.getDepartmentUid() == null)
                return Response.error("Department is required");

            if (vehicleRepository.existsByPlateNumber(dto.getPlateNumber()))
                return Response.error("A vehicle with plate number '" + dto.getPlateNumber() + "' already exists");

            Optional<PoliceStation> station = stationRepository.findByUid(dto.getStationUid());
            if (station.isEmpty()) return Response.error("Station not found");

            Optional<Department> dept = departmentRepository.findByUid(dto.getDepartmentUid());
            if (dept.isEmpty()) return Response.error("Department not found");

            vehicle = EmergencyVehicle.builder()
                    .plateNumber(dto.getPlateNumber().toUpperCase().trim())
                    .model(dto.getModel().trim())
                    .vehicleType(dto.getVehicleType())
                    .status(VehicleStatus.AVAILABLE)
                    .waterCapacityLitres(dto.getWaterCapacityLitres())
                    .equipmentList(dto.getEquipmentList())
                    .hasAdvancedLifeSupport(Boolean.TRUE.equals(dto.getHasAdvancedLifeSupport()))
                    .stretcherCapacity(dto.getStretcherCapacity() != null ? dto.getStretcherCapacity() : 1)
                    .station(station.get())
                    .department(dept.get())
                    .build();
        }

        try {
            EmergencyVehicle saved = vehicleRepository.save(vehicle);
            log.info("Saved emergency vehicle: {} ({})", saved.getPlateNumber(), saved.getVehicleType());
            return Response.success(saved);
        } catch (Exception e) {
            log.error("Failed to save vehicle: {}", e.getMessage());
            return Response.error("Failed to save vehicle: " + Utils.getExceptionMessage(e));
        }
    }

    public Response<EmergencyVehicle> getVehicle(String uid) {
        if (uid == null) return Response.error("UID is required");
        return vehicleRepository.findByUid(uid)
                .map(Response::new)
                .orElseGet(() -> Response.error("Vehicle not found"));
    }

    @Transactional
    public Response<EmergencyVehicle> deleteVehicle(String uid) {
        if (uid == null) return Response.error("UID is required");
        Optional<EmergencyVehicle> opt = vehicleRepository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Vehicle not found");
        EmergencyVehicle v = opt.get();
        if (!v.getIsActive()) return Response.error("Vehicle already deleted");
        v.delete();
        vehicleRepository.save(v);
        log.info("Deleted vehicle: {}", v.getPlateNumber());
        return Response.success(v);
    }

    public ResponsePage<EmergencyVehicle> getVehicles(PageableParam param) {
        String stationUid = LoggedUser.getStationUid(); // null for ROOT → no filter
        return new ResponsePage<>(vehicleRepository.findVehicles(
                param.getPageable(false), param.getIsActive(), param.key(), stationUid));
    }

    public ResponsePage<EmergencyVehicle> getVehiclesByStation(PageableParam param, String stationUid) {
        return new ResponsePage<>(vehicleRepository.findVehicles(
                param.getPageable(false), param.getIsActive(), param.key(), stationUid));
    }

    public ResponseList<EmergencyVehicle> getAvailableVehicles(String vehicleTypeStr) {
        String stationUid = LoggedUser.getStationUid();
        if (stationUid == null) return ResponseList.error("Station context missing");
        VehicleType type = null;
        if (vehicleTypeStr != null) {
            try { type = VehicleType.valueOf(vehicleTypeStr.toUpperCase()); }
            catch (IllegalArgumentException e) { return ResponseList.error("Invalid vehicle type"); }
        }
        List<EmergencyVehicle> available = vehicleRepository.findAvailableAtStation(stationUid, type);
        return new ResponseList<>(available);
    }

    @Transactional
    public Response<EmergencyVehicle> updateStatus(String uid, String statusStr) {
        if (uid == null) return Response.error("UID is required");
        if (statusStr == null) return Response.error("Status is required");
        VehicleStatus status;
        try { status = VehicleStatus.valueOf(statusStr.toUpperCase()); }
        catch (IllegalArgumentException e) { return Response.error("Invalid status value"); }

        Optional<EmergencyVehicle> opt = vehicleRepository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Vehicle not found");
        EmergencyVehicle v = opt.get();
        v.setStatus(status);
        v.update();
        vehicleRepository.save(v);
        return Response.success(v);
    }
}
