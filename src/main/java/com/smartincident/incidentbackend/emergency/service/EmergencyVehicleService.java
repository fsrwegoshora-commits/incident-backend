package com.smartincident.incidentbackend.emergency.service;

import com.smartincident.incidentbackend.emergency.entity.EmergencyVehicle;
import com.smartincident.incidentbackend.emergency.repository.EmergencyUnitRepository;
import com.smartincident.incidentbackend.emergency.repository.EmergencyVehicleRepository;
import com.smartincident.incidentbackend.enums.VehicleStatus;
import com.smartincident.incidentbackend.enums.VehicleType;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Read-only / cross-cutting operations on the polymorphic EmergencyVehicle hierarchy.
 * Vehicle creation and type-specific updates are handled by:
 *   - FireVehicleService  → /api/fire/vehicles
 *   - AmbulanceService    → /api/medical/ambulances
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmergencyVehicleService {

    private final EmergencyVehicleRepository vehicleRepository;
    private final EmergencyUnitRepository emergencyUnitRepository;

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
        String unitUid = LoggedUser.getEmergencyUnitUid();
        return new ResponsePage<>(vehicleRepository.findVehicles(
                param.getPageable(false), param.getIsActive(), param.key(), unitUid));
    }

    public ResponsePage<EmergencyVehicle> getVehiclesByUnit(PageableParam param, String unitUid) {
        return new ResponsePage<>(vehicleRepository.findVehicles(
                param.getPageable(false), param.getIsActive(), param.key(), unitUid));
    }

    public ResponseList<EmergencyVehicle> getAvailableVehicles(String vehicleTypeStr) {
        String unitUid = LoggedUser.getEmergencyUnitUid();
        if (unitUid == null) return ResponseList.error("Emergency unit context missing");
        VehicleType type = null;
        if (vehicleTypeStr != null) {
            try { type = VehicleType.valueOf(vehicleTypeStr.toUpperCase()); }
            catch (IllegalArgumentException e) { return ResponseList.error("Invalid vehicle type"); }
        }
        List<EmergencyVehicle> available = vehicleRepository.findAvailableAtStation(unitUid, type);
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
