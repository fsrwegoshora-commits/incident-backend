package com.smartincident.incidentbackend.gis.service;

import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.emergency.entity.EmergencyVehicle;
import com.smartincident.incidentbackend.emergency.repository.EmergencyUnitRepository;
import com.smartincident.incidentbackend.emergency.repository.EmergencyVehicleRepository;
import com.smartincident.incidentbackend.emergency.repository.VehicleLocationRepository;
import com.smartincident.incidentbackend.enums.*;
import com.smartincident.incidentbackend.fire.repository.FireOfficerRepository;
import com.smartincident.incidentbackend.gis.dto.HeatmapDto;
import com.smartincident.incidentbackend.gis.dto.LiveResourceDto;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.incident.repository.IncidentReportRepository;
import com.smartincident.incidentbackend.medical.repository.MedicRepository;
import com.smartincident.incidentbackend.utils.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GisService {

    private final IncidentReportRepository incidentRepository;
    private final EmergencyVehicleRepository vehicleRepository;
    private final EmergencyUnitRepository emergencyUnitRepository;
    private final VehicleLocationRepository vehicleLocationRepository;
    private final FireOfficerRepository fireOfficerRepository;
    private final MedicRepository medicRepository;

    private static final List<IncidentStatus> ACTIVE_STATUSES = List.of(
            IncidentStatus.REPORTED, IncidentStatus.UNDER_REVIEW, IncidentStatus.CLASSIFIED,
            IncidentStatus.WAITING_FOR_DISPATCH, IncidentStatus.DISPATCHED,
            IncidentStatus.ACKNOWLEDGED, IncidentStatus.EN_ROUTE,
            IncidentStatus.AT_SCENE, IncidentStatus.IN_PROGRESS);

    // ── Live Resource Map ─────────────────────────────────────────────────────

    public Response<LiveResourceDto> getLiveResources() {
        List<EmergencyVehicle> allVehicles = vehicleRepository.findAll().stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsActive())
                        && v.getStatus() != VehicleStatus.OUT_OF_SERVICE)
                .toList();

        // Build vehicle markers with last known GPS
        List<LiveResourceDto.VehicleMarker> vehicleMarkers = allVehicles.stream()
                .filter(v -> v.getLatitude() != null && v.getLongitude() != null)
                .map(v -> {
                    // Find active incident linked to this vehicle
                    String activeIncidentUid = null;
                    String activeIncidentTitle = null;
                    if (v.getStatus() == VehicleStatus.DISPATCHED
                            || v.getStatus() == VehicleStatus.EN_ROUTE
                            || v.getStatus() == VehicleStatus.ON_SCENE) {
                        var dispatch = vehicleLocationRepository.findLatestByVehicleUid(v.getUid());
                        if (dispatch.isPresent() && dispatch.get().getIncident() != null) {
                            activeIncidentUid   = dispatch.get().getIncident().getUid();
                            activeIncidentTitle = dispatch.get().getIncident().getTitle();
                        }
                    }
                    return LiveResourceDto.VehicleMarker.builder()
                            .uid(v.getUid())
                            .plateNumber(v.getPlateNumber())
                            .vehicleType(v.getVehicleType() != null ? v.getVehicleType().name() : null)
                            .status(v.getStatus().name())
                            .latitude(v.getLatitude())
                            .longitude(v.getLongitude())
                            .lastUpdated(v.getLastLocationUpdate())
                            .unitName(v.getEmergencyUnit() != null ? v.getEmergencyUnit().getName() : null)
                            .agencyType(v.getEmergencyUnit() != null && v.getEmergencyUnit().getAgency() != null
                                    ? v.getEmergencyUnit().getAgency().getAgencyType() : null)
                            .activeIncidentUid(activeIncidentUid)
                            .activeIncidentTitle(activeIncidentTitle)
                            .build();
                })
                .toList();

        // Active incident markers
        List<LiveResourceDto.IncidentMarker> incidentMarkers = incidentRepository.findAll().stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsActive())
                        && ACTIVE_STATUSES.contains(i.getStatus())
                        && i.getLatitude() != null && i.getLongitude() != null)
                .map(i -> LiveResourceDto.IncidentMarker.builder()
                        .uid(i.getUid())
                        .title(i.getTitle())
                        .type(i.getType() != null ? i.getType().name() : null)
                        .status(i.getStatus().name())
                        .emergencyLevel(i.getEmergencyLevel() != null ? i.getEmergencyLevel().name() : null)
                        .latitude(i.getLatitude())
                        .longitude(i.getLongitude())
                        .reportedAt(i.getReportedAt())
                        .slaBreached(i.getSlaBreachedAt() != null)
                        .assignedUnitName(i.getAssignedUnit() != null ? i.getAssignedUnit().getName() : null)
                        .assignedStationName(i.getAssignedPoliceStation() != null ? i.getAssignedPoliceStation().getName() : null)
                        .build())
                .toList();

        // Unit markers (stations and emergency units)
        List<LiveResourceDto.UnitMarker> unitMarkers = emergencyUnitRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsActive())
                        && u.getLocation() != null
                        && u.getLocation().getLatitude() != null)
                .map(u -> buildUnitMarker(u))
                .toList();

        return Response.success(LiveResourceDto.builder()
                .vehicles(vehicleMarkers)
                .activeIncidents(incidentMarkers)
                .units(unitMarkers)
                .build());
    }

    private LiveResourceDto.UnitMarker buildUnitMarker(EmergencyUnit u) {
        int fire = (int) fireOfficerRepository.findAll().stream()
                .filter(f -> Boolean.TRUE.equals(f.getIsOnDuty()) && Boolean.TRUE.equals(f.getIsActive())
                        && f.getEmergencyUnit() != null && u.getUid().equals(f.getEmergencyUnit().getUid()))
                .count();
        int medic = (int) medicRepository.findAll().stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsOnDuty()) && Boolean.TRUE.equals(m.getIsActive())
                        && m.getEmergencyUnit() != null && u.getUid().equals(m.getEmergencyUnit().getUid()))
                .count();
        int available = vehicleRepository.findByUnitUidAndStatus(u.getUid(), VehicleStatus.AVAILABLE).size();
        long activeInc = incidentRepository.findAll().stream()
                .filter(i -> i.getAssignedUnit() != null && u.getUid().equals(i.getAssignedUnit().getUid())
                        && Boolean.TRUE.equals(i.getIsActive()) && ACTIVE_STATUSES.contains(i.getStatus()))
                .count();

        return LiveResourceDto.UnitMarker.builder()
                .uid(u.getUid())
                .name(u.getName())
                .unitType(u.getUnitType() != null ? u.getUnitType().name() : null)
                .agencyType(u.getAgency() != null ? u.getAgency().getAgencyType() : null)
                .latitude(u.getLocation().getLatitude())
                .longitude(u.getLocation().getLongitude())
                .onDutyPersonnel(fire + medic)
                .availableVehicles(available)
                .activeIncidents((int) activeInc)
                .build();
    }

    // ── GIS Heatmap ───────────────────────────────────────────────────────────

    /**
     * Groups incidents into geographic grid cells and returns density data.
     *
     * @param incidentType optional filter by IncidentType name (e.g., "THEFT")
     * @param days         look-back window (default 30)
     * @param gridSize     grid cell size in degrees (default 0.05 ≈ 5.5 km)
     */
    public Response<HeatmapDto> getHeatmap(String incidentType, int days, double gridSize) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        List<IncidentReport> incidents = incidentRepository.findAll().stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsActive())
                        && i.getReportedAt() != null && i.getReportedAt().isAfter(since)
                        && i.getLatitude() != null && i.getLongitude() != null)
                .filter(i -> incidentType == null || incidentType.isBlank()
                        || (i.getType() != null && i.getType().name().equalsIgnoreCase(incidentType)))
                .toList();

        // Group by grid cell
        Map<String, List<IncidentReport>> grouped = incidents.stream()
                .collect(Collectors.groupingBy(i -> gridKey(i.getLatitude(), i.getLongitude(), gridSize)));

        // Max count for normalisation
        int maxCount = grouped.values().stream().mapToInt(List::size).max().orElse(1);

        List<HeatmapDto.HeatCell> cells = grouped.entrySet().stream()
                .map(e -> {
                    List<IncidentReport> group = e.getValue();
                    double[] center = gridCenter(e.getKey(), gridSize);
                    int count = group.size();

                    Map<String, Long> byType = group.stream()
                            .filter(i -> i.getType() != null)
                            .collect(Collectors.groupingBy(i -> i.getType().name(), Collectors.counting()));
                    Map<String, Long> byLevel = group.stream()
                            .filter(i -> i.getEmergencyLevel() != null)
                            .collect(Collectors.groupingBy(i -> i.getEmergencyLevel().name(), Collectors.counting()));

                    int intensity = (int) Math.ceil(5.0 * count / maxCount);
                    return HeatmapDto.HeatCell.builder()
                            .latitude(center[0])
                            .longitude(center[1])
                            .count(count)
                            .intensity(Math.max(1, intensity))
                            .byType(byType)
                            .byLevel(byLevel)
                            .build();
                })
                .sorted(Comparator.comparingInt(HeatmapDto.HeatCell::getCount).reversed())
                .toList();

        return Response.success(HeatmapDto.builder()
                .totalIncidents(incidents.size())
                .days(days)
                .gridSizeDegrees(gridSize)
                .cells(cells)
                .build());
    }

    private String gridKey(double lat, double lng, double gridSize) {
        long latCell = (long) Math.floor(lat / gridSize);
        long lngCell = (long) Math.floor(lng / gridSize);
        return latCell + ":" + lngCell;
    }

    private double[] gridCenter(String key, double gridSize) {
        String[] parts = key.split(":");
        double latCell = Double.parseDouble(parts[0]);
        double lngCell = Double.parseDouble(parts[1]);
        return new double[]{
                (latCell + 0.5) * gridSize,
                (lngCell + 0.5) * gridSize
        };
    }
}
