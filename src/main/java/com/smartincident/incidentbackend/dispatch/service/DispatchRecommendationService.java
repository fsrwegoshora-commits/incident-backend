package com.smartincident.incidentbackend.dispatch.service;

import com.smartincident.incidentbackend.dispatch.dto.DispatchRecommendationResult;
import com.smartincident.incidentbackend.dispatch.dto.UnitRecommendation;
import com.smartincident.incidentbackend.dispatch.dto.VehicleRecommendation;
import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.emergency.entity.EmergencyVehicle;
import com.smartincident.incidentbackend.emergency.repository.EmergencyUnitRepository;
import com.smartincident.incidentbackend.emergency.repository.EmergencyVehicleRepository;
import com.smartincident.incidentbackend.enums.UnitType;
import com.smartincident.incidentbackend.enums.VehicleStatus;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.incident.repository.IncidentReportRepository;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
import com.smartincident.incidentbackend.police.repository.PoliceStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DispatchRecommendationService {

    private final IncidentReportRepository incidentRepo;
    private final PoliceStationRepository  policeStationRepo;
    private final EmergencyUnitRepository  emergencyUnitRepo;
    private final EmergencyVehicleRepository vehicleRepo;

    /** Average response speeds (km/h) per responder type. */
    private static final double POLICE_SPEED_KMH   = 80.0;
    private static final double FIRE_SPEED_KMH     = 60.0;
    private static final double MEDICAL_SPEED_KMH  = 90.0;

    private static final Set<UnitType> FIRE_UNIT_TYPES = Set.of(
            UnitType.FIRE_BRIGADE, UnitType.FIRE_DISTRICT_STATION,
            UnitType.FIRE_REGIONAL_COMMAND, UnitType.FIRE_NATIONAL_HQ,
            UnitType.AIRPORT_FIRE_UNIT
    );

    private static final Set<UnitType> MEDICAL_UNIT_TYPES = Set.of(
            UnitType.HOSPITAL_AMBULANCE_UNIT
    );

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Given an incident UID, find the nearest available responders for each
     * agency required by the incident and return sorted recommendations with ETA.
     */
    public DispatchRecommendationResult recommend(String incidentUid) {
        IncidentReport incident = incidentRepo.findByUid(incidentUid)
                .orElseThrow(() -> new RuntimeException("Incident not found: " + incidentUid));

        if (incident.getLatitude() == null || incident.getLongitude() == null) {
            throw new RuntimeException("Incident has no GPS coordinates");
        }

        double lat = incident.getLatitude();
        double lng = incident.getLongitude();

        UnitRecommendation policeRec  = null;
        UnitRecommendation fireRec    = null;
        UnitRecommendation medicalRec = null;

        if (Boolean.TRUE.equals(incident.getRequiresPoliceService())) {
            policeRec = findNearestPoliceStation(lat, lng);
        }
        if (Boolean.TRUE.equals(incident.getRequiresFireService())) {
            fireRec = findNearestEmergencyUnit(lat, lng, FIRE_UNIT_TYPES, FIRE_SPEED_KMH, "FIRE_UNIT");
        }
        if (Boolean.TRUE.equals(incident.getRequiresMedicalService())) {
            medicalRec = findNearestEmergencyUnit(lat, lng, MEDICAL_UNIT_TYPES, MEDICAL_SPEED_KMH, "MEDICAL_UNIT");
        }

        List<VehicleRecommendation> vehicles = collectAvailableVehicles(policeRec, fireRec, medicalRec, lat, lng);

        return DispatchRecommendationResult.builder()
                .incidentUid(incidentUid)
                .incidentLatitude(lat)
                .incidentLongitude(lng)
                .nearestPoliceStation(policeRec)
                .nearestFireUnit(fireRec)
                .nearestMedicalUnit(medicalRec)
                .availableVehicles(vehicles)
                .totalAvailableVehicles(vehicles.size())
                .build();
    }

    /**
     * Ad-hoc recommendation given coordinates (for dispatcher without an incident yet).
     */
    public DispatchRecommendationResult recommendByCoords(double lat, double lng,
                                                          boolean needsPolice,
                                                          boolean needsFire,
                                                          boolean needsMedical) {
        UnitRecommendation policeRec  = needsPolice  ? findNearestPoliceStation(lat, lng) : null;
        UnitRecommendation fireRec    = needsFire    ? findNearestEmergencyUnit(lat, lng, FIRE_UNIT_TYPES,    FIRE_SPEED_KMH,    "FIRE_UNIT")    : null;
        UnitRecommendation medicalRec = needsMedical ? findNearestEmergencyUnit(lat, lng, MEDICAL_UNIT_TYPES, MEDICAL_SPEED_KMH, "MEDICAL_UNIT") : null;

        List<VehicleRecommendation> vehicles = collectAvailableVehicles(policeRec, fireRec, medicalRec, lat, lng);

        return DispatchRecommendationResult.builder()
                .incidentLatitude(lat)
                .incidentLongitude(lng)
                .nearestPoliceStation(policeRec)
                .nearestFireUnit(fireRec)
                .nearestMedicalUnit(medicalRec)
                .availableVehicles(vehicles)
                .totalAvailableVehicles(vehicles.size())
                .build();
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private UnitRecommendation findNearestPoliceStation(double lat, double lng) {
        List<PoliceStation> stations = policeStationRepo.findByIsActiveTrue();
        return stations.stream()
                .filter(s -> s.getLocation() != null
                        && s.getLocation().getLatitude() != null
                        && s.getLocation().getLongitude() != null)
                .map(s -> {
                    double dist = haversine(lat, lng,
                            s.getLocation().getLatitude(), s.getLocation().getLongitude());
                    int eta = etaMinutes(dist, POLICE_SPEED_KMH);
                    return UnitRecommendation.builder()
                            .uid(s.getUid())
                            .name(s.getName())
                            .type("POLICE_STATION")
                            .distanceKm(round2(dist))
                            .etaMinutes(eta)
                            .latitude(s.getLocation().getLatitude())
                            .longitude(s.getLocation().getLongitude())
                            .availableVehicles(0)
                            .build();
                })
                .min(Comparator.comparingDouble(UnitRecommendation::getDistanceKm))
                .orElse(null);
    }

    private UnitRecommendation findNearestEmergencyUnit(double lat, double lng,
                                                         Set<UnitType> types,
                                                         double speedKmh,
                                                         String unitTypeLabel) {
        List<EmergencyUnit> units = emergencyUnitRepo.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsActive())
                        && types.contains(u.getUnitType())
                        && u.getLocation() != null
                        && u.getLocation().getLatitude() != null
                        && u.getLocation().getLongitude() != null)
                .collect(Collectors.toList());

        return units.stream()
                .map(u -> {
                    double dist = haversine(lat, lng,
                            u.getLocation().getLatitude(), u.getLocation().getLongitude());
                    int eta = etaMinutes(dist, speedKmh);
                    long avail = vehicleRepo.findByUnitUidAndStatus(u.getUid(), VehicleStatus.AVAILABLE).size();
                    return UnitRecommendation.builder()
                            .uid(u.getUid())
                            .name(u.getName())
                            .type(unitTypeLabel)
                            .distanceKm(round2(dist))
                            .etaMinutes(eta)
                            .latitude(u.getLocation().getLatitude())
                            .longitude(u.getLocation().getLongitude())
                            .availableVehicles((int) avail)
                            .build();
                })
                .min(Comparator.comparingDouble(UnitRecommendation::getDistanceKm))
                .orElse(null);
    }

    private List<VehicleRecommendation> collectAvailableVehicles(
            UnitRecommendation police, UnitRecommendation fire, UnitRecommendation medical,
            double incidentLat, double incidentLng) {

        List<VehicleRecommendation> all = new ArrayList<>();

        for (UnitRecommendation unit : List.of(
                police != null ? police : null,
                fire   != null ? fire   : null,
                medical!= null ? medical: null)) {
            if (unit == null) continue;

            // Only emergency units have vehicles (police station doesn't in this model)
            if ("POLICE_STATION".equals(unit.getType())) continue;

            List<EmergencyVehicle> vehicles = vehicleRepo.findByUnitUidAndStatus(unit.getUid(), VehicleStatus.AVAILABLE);
            for (EmergencyVehicle v : vehicles) {
                double dist = (v.getLatitude() != null && v.getLongitude() != null)
                        ? haversine(incidentLat, incidentLng, v.getLatitude(), v.getLongitude())
                        : unit.getDistanceKm();
                double speed = "FIRE_UNIT".equals(unit.getType()) ? FIRE_SPEED_KMH : MEDICAL_SPEED_KMH;

                all.add(VehicleRecommendation.builder()
                        .uid(v.getUid())
                        .plateNumber(v.getPlateNumber())
                        .model(v.getModel())
                        .vehicleType(v.getVehicleType() != null ? v.getVehicleType().name() : null)
                        .status(v.getStatus().name())
                        .unitName(unit.getName())
                        .unitUid(unit.getUid())
                        .distanceKm(round2(dist))
                        .etaMinutes(etaMinutes(dist, speed))
                        .build());
            }
        }

        all.sort(Comparator.comparingDouble(VehicleRecommendation::getDistanceKm));
        return all.stream().limit(10).collect(Collectors.toList());
    }

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private static int etaMinutes(double distanceKm, double speedKmh) {
        return (int) Math.ceil((distanceKm / speedKmh) * 60);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
