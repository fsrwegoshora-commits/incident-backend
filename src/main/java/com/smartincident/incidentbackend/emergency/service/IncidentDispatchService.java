package com.smartincident.incidentbackend.emergency.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.emergency.dto.DispatchDto;
import com.smartincident.incidentbackend.emergency.entity.EmergencyVehicle;
import com.smartincident.incidentbackend.emergency.entity.IncidentDispatch;
import com.smartincident.incidentbackend.emergency.entity.VehicleShift;
import com.smartincident.incidentbackend.emergency.repository.EmergencyVehicleRepository;
import com.smartincident.incidentbackend.emergency.repository.IncidentDispatchRepository;
import com.smartincident.incidentbackend.emergency.repository.VehicleShiftRepository;
import com.smartincident.incidentbackend.enums.DispatchStatus;
import com.smartincident.incidentbackend.enums.VehicleStatus;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.incident.repository.IncidentReportRepository;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentDispatchService {

    private final IncidentDispatchRepository dispatchRepository;
    private final EmergencyVehicleRepository vehicleRepository;
    private final IncidentReportRepository incidentRepository;
    private final UserRepository userRepository;
    private final VehicleShiftRepository vehicleShiftRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Response<IncidentDispatch> dispatch(DispatchDto dto) {
        if (dto == null) return Response.error("Dispatch DTO cannot be null");
        if (dto.getIncidentUid() == null) return Response.error("Incident UID is required");
        if (dto.getVehicleUid() == null) return Response.error("Vehicle UID is required");

        Optional<IncidentReport> incidentOpt = incidentRepository.findByUid(dto.getIncidentUid());
        if (incidentOpt.isEmpty()) return Response.error("Incident not found");

        Optional<EmergencyVehicle> vehicleOpt = vehicleRepository.findByUid(dto.getVehicleUid());
        if (vehicleOpt.isEmpty()) return Response.error("Vehicle not found");

        EmergencyVehicle vehicle = vehicleOpt.get();
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE)
            return Response.error("Vehicle is not available for dispatch (current status: " + vehicle.getStatus() + ")");

        // Resolve dispatcher
        String dispatcherUid = LoggedUser.getUid();
        User dispatcher = dispatcherUid != null
                ? userRepository.findByUid(dispatcherUid).orElse(null)
                : null;

        // Resolve crew from today's active VehicleShift (or use DTO overrides)
        String driverName = null;
        String commanderName = null;
        String crewJson = null;

        List<VehicleShift> activeShifts = vehicleShiftRepository
                .findByVehicleUidAndDate(dto.getVehicleUid(), LocalDate.now());
        VehicleShift shift = activeShifts.isEmpty() ? null : activeShifts.get(0);

        if (shift != null) {
            // Police vehicle
            if (shift.getCrew() != null && !shift.getCrew().isEmpty()) {
                List<String> names = shift.getCrew().stream()
                        .filter(o -> o.getUserAccount() != null)
                        .map(o -> o.getUserAccount().getName())
                        .collect(Collectors.toList());
                crewJson = toJson(names);
                if (!names.isEmpty()) driverName = names.get(0);
            }
            // Fire vehicle
            if (shift.getFireCrewDriver() != null && shift.getFireCrewDriver().getUserAccount() != null) {
                driverName = shift.getFireCrewDriver().getUserAccount().getName();
            }
            if (shift.getFireCrewCommander() != null && shift.getFireCrewCommander().getUserAccount() != null) {
                commanderName = shift.getFireCrewCommander().getUserAccount().getName();
            }
            if (shift.getFireCrew() != null && !shift.getFireCrew().isEmpty()) {
                List<String> names = shift.getFireCrew().stream()
                        .filter(o -> o.getUserAccount() != null)
                        .map(o -> o.getUserAccount().getName())
                        .collect(Collectors.toList());
                crewJson = toJson(names);
            }
            // Ambulance
            if (shift.getDriver() != null && shift.getDriver().getUserAccount() != null) {
                driverName = shift.getDriver().getUserAccount().getName();
            }
            if (shift.getInCharge() != null && shift.getInCharge().getUserAccount() != null) {
                commanderName = shift.getInCharge().getUserAccount().getName();
            }
            if (shift.getMedics() != null && !shift.getMedics().isEmpty()) {
                List<String> names = shift.getMedics().stream()
                        .filter(m -> m.getUserAccount() != null)
                        .map(m -> m.getUserAccount().getName())
                        .collect(Collectors.toList());
                crewJson = toJson(names);
            }
        }

        IncidentDispatch dispatch = IncidentDispatch.builder()
                .incident(incidentOpt.get())
                .vehicle(vehicle)
                .dispatchedBy(dispatcher)
                .status(DispatchStatus.PENDING)
                .dispatchedAt(LocalDateTime.now())
                .notes(dto.getNotes())
                .vehiclePlate(vehicle.getPlateNumber())
                .driverName(driverName)
                .commanderName(commanderName)
                .crewJson(crewJson)
                .build();

        // Mark vehicle as dispatched
        vehicle.setStatus(VehicleStatus.DISPATCHED);
        vehicleRepository.save(vehicle);

        try {
            IncidentDispatch saved = dispatchRepository.save(dispatch);
            log.info("Dispatched vehicle {} to incident {}", vehicle.getPlateNumber(), incidentOpt.get().getUid());
            return Response.success(saved);
        } catch (Exception e) {
            log.error("Failed to create dispatch: {}", e.getMessage());
            return Response.error("Failed to create dispatch: " + Utils.getExceptionMessage(e));
        }
    }

    @Transactional
    public Response<IncidentDispatch> updateStatus(String uid, DispatchDto dto) {
        if (uid == null) return Response.error("UID is required");
        if (dto == null || dto.getStatus() == null) return Response.error("Status is required");

        Optional<IncidentDispatch> opt = dispatchRepository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Dispatch not found");

        IncidentDispatch dispatch = opt.get();
        DispatchStatus newStatus = dto.getStatus();
        dispatch.setStatus(newStatus);

        if (dto.getEtaMinutes() != null) dispatch.setEtaMinutes(dto.getEtaMinutes());

        LocalDateTime now = LocalDateTime.now();
        switch (newStatus) {
            case ACKNOWLEDGED -> dispatch.setAcknowledgedAt(now);
            case ON_SCENE     -> dispatch.setArrivedAt(now);
            case CLEARED      -> {
                dispatch.setClearedAt(now);
                // Free the vehicle
                dispatch.getVehicle().setStatus(VehicleStatus.RETURNING);
                vehicleRepository.save(dispatch.getVehicle());
            }
            case CANCELLED    -> {
                // Release vehicle immediately
                dispatch.getVehicle().setStatus(VehicleStatus.AVAILABLE);
                vehicleRepository.save(dispatch.getVehicle());
            }
            default -> {}
        }

        // Sync vehicle status with dispatch
        if (newStatus == DispatchStatus.EN_ROUTE) {
            dispatch.getVehicle().setStatus(VehicleStatus.EN_ROUTE);
            vehicleRepository.save(dispatch.getVehicle());
        }

        dispatchRepository.save(dispatch);
        return Response.success(dispatch);
    }

    public ResponseList<IncidentDispatch> getByIncident(String incidentUid) {
        return new ResponseList<>(dispatchRepository.findByIncidentUidOrderByDispatchedAtDesc(incidentUid));
    }

    public ResponseList<IncidentDispatch> getByVehicle(String vehicleUid) {
        return new ResponseList<>(dispatchRepository.findByVehicleUidOrderByDispatchedAtDesc(vehicleUid));
    }

    public ResponseList<IncidentDispatch> getActiveByStation(String stationUid) {
        if (stationUid == null) stationUid = LoggedUser.getStationUid();
        if (stationUid == null) return ResponseList.error("Station context missing");
        return new ResponseList<>(dispatchRepository.findActiveDispatchesByStation(stationUid));
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return null;
        }
    }
}
