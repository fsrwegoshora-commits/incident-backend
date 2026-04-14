package com.smartincident.incidentbackend.emergency.service;

import com.smartincident.incidentbackend.emergency.dto.VehicleLocationBroadcast;
import com.smartincident.incidentbackend.emergency.dto.VehicleLocationDto;
import com.smartincident.incidentbackend.emergency.entity.EmergencyVehicle;
import com.smartincident.incidentbackend.emergency.entity.IncidentDispatch;
import com.smartincident.incidentbackend.emergency.entity.VehicleLocation;
import com.smartincident.incidentbackend.emergency.repository.EmergencyVehicleRepository;
import com.smartincident.incidentbackend.emergency.repository.IncidentDispatchRepository;
import com.smartincident.incidentbackend.emergency.repository.VehicleLocationRepository;
import com.smartincident.incidentbackend.enums.DispatchStatus;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.incident.repository.IncidentReportRepository;
import com.smartincident.incidentbackend.utils.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleLocationService {

    private final VehicleLocationRepository locationRepository;
    private final EmergencyVehicleRepository vehicleRepository;
    private final IncidentReportRepository incidentRepository;
    private final IncidentDispatchRepository dispatchRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Called when a vehicle crew sends a GPS ping.
     * Saves the location, updates vehicle's last-known position,
     * calculates ETA, then broadcasts over WebSocket.
     */
    @Transactional
    public Response<VehicleLocationBroadcast> recordLocation(VehicleLocationDto dto) {
        if (dto == null || dto.getVehicleUid() == null)
            return Response.error("Vehicle UID is required");
        if (dto.getLatitude() == null || dto.getLongitude() == null)
            return Response.error("Coordinates are required");

        Optional<EmergencyVehicle> vehicleOpt = vehicleRepository.findByUid(dto.getVehicleUid());
        if (vehicleOpt.isEmpty()) return Response.error("Vehicle not found");

        EmergencyVehicle vehicle = vehicleOpt.get();

        // Resolve active incident if provided
        IncidentReport incident = null;
        if (dto.getIncidentUid() != null) {
            incident = incidentRepository.findByUid(dto.getIncidentUid()).orElse(null);
        }

        // Persist GPS ping
        VehicleLocation location = VehicleLocation.builder()
                .vehicle(vehicle)
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .speedKmh(dto.getSpeedKmh())
                .heading(dto.getHeading())
                .recordedAt(LocalDateTime.now())
                .incident(incident)
                .build();
        locationRepository.save(location);

        // Update vehicle's last known position
        vehicle.setLatitude(dto.getLatitude());
        vehicle.setLongitude(dto.getLongitude());
        vehicle.setLastLocationUpdate(LocalDateTime.now());
        vehicleRepository.save(vehicle);

        // Find the active dispatch for ETA calculation
        IncidentDispatch activeDispatch = findActiveDispatch(vehicle.getUid());
        Integer eta = null;
        String dispatchStatus = null;
        String incidentUid = null;

        if (activeDispatch != null) {
            dispatchStatus = activeDispatch.getStatus().name();
            incidentUid = activeDispatch.getIncident().getUid();

            // Calculate ETA using Haversine formula
            if (activeDispatch.getIncident().getLatitude() != null
                    && activeDispatch.getIncident().getLongitude() != null) {
                double distanceKm = haversineKm(
                        dto.getLatitude(), dto.getLongitude(),
                        activeDispatch.getIncident().getLatitude(),
                        activeDispatch.getIncident().getLongitude());

                double speedKmh = dto.getSpeedKmh() != null && dto.getSpeedKmh() > 5
                        ? dto.getSpeedKmh() : 40.0; // assume 40 km/h if speed unknown
                eta = (int) Math.ceil((distanceKm / speedKmh) * 60);

                // Persist ETA on dispatch
                activeDispatch.setEtaMinutes(eta);
                dispatchRepository.save(activeDispatch);
            }
        }

        // Build broadcast payload
        VehicleLocationBroadcast broadcast = VehicleLocationBroadcast.builder()
                .vehicleUid(vehicle.getUid())
                .plateNumber(vehicle.getPlateNumber())
                .vehicleType(vehicle.getVehicleType())
                .vehicleStatus(vehicle.getStatus())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .speedKmh(dto.getSpeedKmh())
                .heading(dto.getHeading())
                .etaMinutes(eta)
                .incidentUid(incidentUid)
                .dispatchStatus(dispatchStatus)
                .recordedAt(location.getRecordedAt())
                .build();

        // Broadcast to vehicle-specific topic
        messagingTemplate.convertAndSend(
                "/topic/vehicle-location/" + vehicle.getUid(), broadcast);

        // Also broadcast to incident topic so dispatchers see all vehicles at once
        if (incidentUid != null) {
            messagingTemplate.convertAndSend(
                    "/topic/incident-dispatch/" + incidentUid, broadcast);
        }

        return Response.success(broadcast);
    }

    public Response<VehicleLocation> getLatestLocation(String vehicleUid) {
        return locationRepository.findLatestByVehicleUid(vehicleUid)
                .map(Response::new)
                .orElseGet(() -> Response.error("No location recorded for this vehicle"));
    }

    public List<VehicleLocation> getTrail(String vehicleUid, int lastMinutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(lastMinutes);
        return locationRepository.findTrailSince(vehicleUid, since);
    }

    /** Purge GPS trail older than 24 hours to keep the table lean. */
    @Scheduled(cron = "0 0 3 * * *") // 03:00 every day
    @Transactional
    public void pruneOldLocations() {
        // JPA bulk-delete per vehicle would be ideal; simple cutoff here
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        try {
            vehicleRepository.findAll().forEach(v ->
                    locationRepository.deleteByVehicleUidAndRecordedAtBefore(v.getUid(), cutoff));
            log.info("Pruned GPS locations older than 24 hours");
        } catch (Exception e) {
            log.warn("GPS prune failed: {}", e.getMessage());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private IncidentDispatch findActiveDispatch(String vehicleUid) {
        List<IncidentDispatch> dispatches = dispatchRepository.findByVehicleUidOrderByDispatchedAtDesc(vehicleUid);
        return dispatches.stream()
                .filter(d -> d.getStatus() != DispatchStatus.CLEARED
                        && d.getStatus() != DispatchStatus.CANCELLED)
                .findFirst()
                .orElse(null);
    }

    /** Haversine formula — returns distance in kilometres. */
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
