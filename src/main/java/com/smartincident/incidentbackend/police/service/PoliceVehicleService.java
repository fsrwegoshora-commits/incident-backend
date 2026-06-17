package com.smartincident.incidentbackend.police.service;

import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.enums.VehicleStatus;
import com.smartincident.incidentbackend.police.dto.PoliceVehicleDto;
import com.smartincident.incidentbackend.police.dto.VehicleAssignmentDto;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
import com.smartincident.incidentbackend.police.entity.TrafficCheckpoint;
import com.smartincident.incidentbackend.police.entity.PoliceVehicle;
import com.smartincident.incidentbackend.police.repository.PoliceStationRepository;
import com.smartincident.incidentbackend.police.repository.PoliceVehicleRepository;
import com.smartincident.incidentbackend.police.repository.TrafficCheckPointRepository;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PoliceVehicleService {

    private final PoliceVehicleRepository vehicleRepository;
    private final PoliceStationRepository stationRepository;
    private final TrafficCheckPointRepository checkpointRepository;

    @Transactional
    public Response<PoliceVehicle> saveVehicle(PoliceVehicleDto dto) {
        if (dto.getPlateNumber() == null || dto.getPlateNumber().isBlank())
            return Response.error("Plate number is required");
        if (dto.getVehicleType() == null)
            return Response.error("Vehicle type is required");

        PoliceVehicle vehicle;

        if (dto.getUid() != null) {
            // Update
            Optional<PoliceVehicle> opt = vehicleRepository.findByUid(dto.getUid());
            if (opt.isEmpty()) return Response.error("Vehicle not found");
            vehicle = opt.get();
        } else {
            // Create
            if (vehicleRepository.existsByPlateNumber(dto.getPlateNumber().toUpperCase())) {
                return Response.error("A vehicle with plate number '" + dto.getPlateNumber() + "' already exists");
            }
            vehicle = new PoliceVehicle();
        }

        // Resolve station
        String stationUid = dto.getPoliceStationUid();
        if (stationUid == null && dto.getUid() == null) {
            // For new vehicles, fall back to the caller's station if STATION_ADMIN
            stationUid = LoggedUser.getPoliceStationUid();
        }
        if (stationUid != null) {
            PoliceStation station = stationRepository.findByUid(stationUid)
                    .orElse(null);
            if (station == null) return Response.error("Police station not found");
            vehicle.setPoliceStation(station);
        } else if (dto.getUid() == null) {
            return Response.error("Police station is required");
        }

        vehicle.setPlateNumber(dto.getPlateNumber().toUpperCase().trim());
        if (dto.getModel()      != null) vehicle.setModel(dto.getModel());
        vehicle.setVehicleType(dto.getVehicleType());
        if (dto.getStatus()     != null) vehicle.setStatus(dto.getStatus());
        if (dto.getDriverName() != null) vehicle.setDriverName(dto.getDriverName());
        if (dto.getYear()       != null) vehicle.setYear(dto.getYear());
        if (dto.getNotes()      != null) vehicle.setNotes(dto.getNotes());

        try {
            return new Response<>(vehicleRepository.save(vehicle));
        } catch (Exception e) {
            log.error("Failed to save police vehicle: {}", e.getMessage());
            return Response.error("Failed to save vehicle: " + e.getMessage());
        }
    }

    public Response<PoliceVehicle> getVehicle(String uid) {
        return vehicleRepository.findByUid(uid)
                .map(Response::new)
                .orElseGet(() -> Response.error("Vehicle not found"));
    }

    @Transactional
    public Response<PoliceVehicle> updateStatus(String uid, String status) {
        Optional<PoliceVehicle> opt = vehicleRepository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Vehicle not found");
        try {
            VehicleStatus vs = VehicleStatus.valueOf(status.toUpperCase());
            opt.get().setStatus(vs);
            return new Response<>(vehicleRepository.save(opt.get()));
        } catch (IllegalArgumentException e) {
            return Response.error("Invalid status: " + status);
        }
    }

    @Transactional
    public Response<PoliceVehicle> assignVehicle(String uid, VehicleAssignmentDto dto) {
        if (dto == null || dto.getAssignmentType() == null)
            return Response.error("Assignment type is required");

        Optional<PoliceVehicle> opt = vehicleRepository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Vehicle not found");
        PoliceVehicle vehicle = opt.get();

        String type = dto.getAssignmentType().toUpperCase();

        switch (type) {
            case "PATROL" -> {
                vehicle.setStatus(VehicleStatus.DISPATCHED);
                String area = dto.getNote() != null && !dto.getNote().isBlank()
                        ? dto.getNote().trim() : null;
                vehicle.setNotes(area != null ? "Patrol — " + area : "On patrol");
            }
            case "CHECKPOINT" -> {
                if (dto.getCheckpointUid() == null || dto.getCheckpointUid().isBlank())
                    return Response.error("checkpointUid is required for CHECKPOINT assignment");
                Optional<TrafficCheckpoint> cpOpt = checkpointRepository.findByUid(dto.getCheckpointUid());
                if (cpOpt.isEmpty()) return Response.error("Checkpoint not found");
                TrafficCheckpoint cp = cpOpt.get();
                vehicle.setStatus(VehicleStatus.DISPATCHED);
                vehicle.setNotes("Checkpoint — " + cp.getName());
            }
            case "RETURN" -> {
                vehicle.setStatus(VehicleStatus.AVAILABLE);
                vehicle.setNotes(null);
            }
            default -> { return Response.error("Invalid assignment type: " + type + ". Use PATROL, CHECKPOINT or RETURN"); }
        }

        try {
            vehicle.update();
            PoliceVehicle saved = vehicleRepository.save(vehicle);
            log.info("Vehicle {} assigned: {} — {}", saved.getPlateNumber(), type, saved.getNotes());
            return Response.success(saved);
        } catch (Exception e) {
            log.error("Failed to assign vehicle: {}", e.getMessage());
            return Response.error("Failed to assign vehicle: " + e.getMessage());
        }
    }

    @Transactional
    public Response<PoliceVehicle> deleteVehicle(String uid) {
        Optional<PoliceVehicle> opt = vehicleRepository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Vehicle not found");
        opt.get().delete();
        vehicleRepository.save(opt.get());
        return Response.success(opt.get());
    }

    public ResponsePage<PoliceVehicle> getVehiclesByStation(PageableParam pageableParam, String stationUid) {
        return new ResponsePage<>(vehicleRepository.findByStation(
                pageableParam.getPageable(true), pageableParam.getIsActive(),
                pageableParam.key(), stationUid));
    }

    public ResponsePage<PoliceVehicle> getVehicles(PageableParam pageableParam) {
        Role role = LoggedUser.getRole();
        if (role == Role.AGENCY_ADMIN) {
            String agencyUid = LoggedUser.getAgencyUid();
            if (agencyUid != null) {
                return new ResponsePage<>(vehicleRepository.findByAgency(
                        pageableParam.getPageable(true), pageableParam.getIsActive(),
                        pageableParam.key(), agencyUid));
            }
        }
        if (role == Role.STATION_ADMIN) {
            String stationUid = LoggedUser.getPoliceStationUid();
            return new ResponsePage<>(vehicleRepository.findByStation(
                    pageableParam.getPageable(true), pageableParam.getIsActive(),
                    pageableParam.key(), stationUid != null ? stationUid : ""));
        }
        // ROOT: return all
        return new ResponsePage<>(vehicleRepository.findAll(pageableParam.getPageable(true)));
    }
}
