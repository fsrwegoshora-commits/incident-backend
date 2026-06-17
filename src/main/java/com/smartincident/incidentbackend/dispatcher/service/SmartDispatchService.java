package com.smartincident.incidentbackend.dispatcher.service;

import com.smartincident.incidentbackend.dispatcher.dto.SmartDispatchRecommendation;
import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.emergency.repository.EmergencyUnitRepository;
import com.smartincident.incidentbackend.emergency.repository.EmergencyVehicleRepository;
import com.smartincident.incidentbackend.enums.*;
import com.smartincident.incidentbackend.fire.repository.FireOfficerRepository;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.incident.repository.IncidentReportRepository;
import com.smartincident.incidentbackend.medical.repository.MedicRepository;
import com.smartincident.incidentbackend.operational.entity.OperationalPost;
import com.smartincident.incidentbackend.operational.repository.OperationalPostRepository;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
import com.smartincident.incidentbackend.police.repository.OfficerShiftRepository;
import com.smartincident.incidentbackend.police.repository.PoliceStationRepository;
import com.smartincident.incidentbackend.utils.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmartDispatchService {

    private final IncidentReportRepository incidentRepository;
    private final PoliceStationRepository policeStationRepository;
    private final EmergencyUnitRepository emergencyUnitRepository;
    private final EmergencyVehicleRepository vehicleRepository;
    private final OfficerShiftRepository officerShiftRepository;
    private final FireOfficerRepository fireOfficerRepository;
    private final MedicRepository medicRepository;
    private final OperationalPostRepository operationalPostRepository;

    private static final double POLICE_SPEED_KMH  = 60.0;
    private static final double FIRE_SPEED_KMH    = 50.0;
    private static final double MEDICAL_SPEED_KMH = 70.0;
    private static final double MAX_SEARCH_RADIUS_KM = 100.0;
    private static final int    TOP_N = 5;

    public Response<SmartDispatchRecommendation> recommend(String incidentUid) {
        Optional<IncidentReport> opt = incidentRepository.findByUid(incidentUid);
        if (opt.isEmpty()) return Response.error("Incident not found");
        IncidentReport incident = opt.get();

        if (incident.getLatitude() == null || incident.getLongitude() == null)
            return Response.error("Incident has no GPS coordinates — cannot compute recommendations");

        double lat = incident.getLatitude();
        double lng = incident.getLongitude();
        IncidentType type = incident.getType();

        List<SmartDispatchRecommendation.UnitRecommendation> policeRecs = new ArrayList<>();
        List<SmartDispatchRecommendation.UnitRecommendation> fireRecs   = new ArrayList<>();
        List<SmartDispatchRecommendation.UnitRecommendation> medRecs    = new ArrayList<>();

        if (Boolean.TRUE.equals(incident.getRequiresPoliceService())) {
            policeRecs = buildPoliceRecommendations(lat, lng, type);
        }
        if (Boolean.TRUE.equals(incident.getRequiresFireService())) {
            fireRecs = buildFireRecommendations(lat, lng);
        }
        if (Boolean.TRUE.equals(incident.getRequiresMedicalService())) {
            medRecs = buildMedicalRecommendations(lat, lng);
        }

        return Response.success(SmartDispatchRecommendation.builder()
                .incidentUid(incident.getUid())
                .incidentTitle(incident.getTitle())
                .incidentLat(lat)
                .incidentLng(lng)
                .emergencyCategory(incident.getEmergencyCategory() != null ? incident.getEmergencyCategory().name() : null)
                .emergencyLevel(incident.getEmergencyLevel() != null ? incident.getEmergencyLevel().name() : null)
                .policeRecommendations(policeRecs)
                .fireRecommendations(fireRecs)
                .medicalRecommendations(medRecs)
                .build());
    }

    public Response<Map<String, Object>> getEta(String vehicleUid, double incidentLat, double incidentLng) {
        var vehicle = vehicleRepository.findByUid(vehicleUid);
        if (vehicle.isEmpty()) return Response.error("Vehicle not found");

        var v = vehicle.get();
        if (v.getLatitude() == null || v.getLongitude() == null)
            return Response.error("Vehicle has no last known GPS position");

        double distKm = haversine(v.getLatitude(), v.getLongitude(), incidentLat, incidentLng);
        double speedKmh = estimateVehicleSpeed(v.getVehicleType());
        double etaMinutes = (distKm / speedKmh) * 60.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("vehicleUid", vehicleUid);
        result.put("vehiclePlate", v.getPlateNumber());
        result.put("vehicleType", v.getVehicleType().name());
        result.put("vehicleStatus", v.getStatus().name());
        result.put("distanceKm", Math.round(distKm * 10.0) / 10.0);
        result.put("etaMinutes", (int) Math.ceil(etaMinutes));
        result.put("estimatedSpeedKmh", (int) speedKmh);
        result.put("vehicleLat", v.getLatitude());
        result.put("vehicleLng", v.getLongitude());

        return Response.success(result);
    }

    // ── Police ────────────────────────────────────────────────────────────────

    /**
     * Routing logic:
     * - Traffic/road incidents  → OperationalPost(POLICE_PATROL_POST) first, Station fallback
     * - Crime incidents         → PoliceStation first, OperationalPost(POLICE_CHECKPOINT) secondary
     * - Other                   → Both mixed, ranked by score
     */
    private List<SmartDispatchRecommendation.UnitRecommendation> buildPoliceRecommendations(
            double lat, double lng, IncidentType type) {

        if (isTrafficIncident(type)) {
            List<SmartDispatchRecommendation.UnitRecommendation> posts =
                    buildPostRecommendations(lat, lng, PostType.POLICE_PATROL_POST, POLICE_SPEED_KMH);
            if (posts.size() >= TOP_N) return posts;
            List<SmartDispatchRecommendation.UnitRecommendation> stations = buildStationRecommendations(lat, lng);
            return mergeAndRank(posts, stations);
        }

        if (isCrimeIncident(type)) {
            List<SmartDispatchRecommendation.UnitRecommendation> stations = buildStationRecommendations(lat, lng);
            if (stations.size() >= TOP_N) return stations;
            List<SmartDispatchRecommendation.UnitRecommendation> checkpoints =
                    buildPostRecommendations(lat, lng, PostType.POLICE_CHECKPOINT, POLICE_SPEED_KMH);
            return mergeAndRank(stations, checkpoints);
        }

        // General: blend posts + stations
        List<SmartDispatchRecommendation.UnitRecommendation> posts =
                buildPostRecommendations(lat, lng, PostType.POLICE_PATROL_POST, POLICE_SPEED_KMH);
        List<SmartDispatchRecommendation.UnitRecommendation> stations = buildStationRecommendations(lat, lng);
        return mergeAndRank(posts, stations);
    }

    private List<SmartDispatchRecommendation.UnitRecommendation> buildStationRecommendations(
            double lat, double lng) {

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        return policeStationRepository.findByIsActiveTrue().stream()
                .filter(s -> hasLocation(s.getLocation()))
                .map(station -> {
                    double dist = haversine(lat, lng,
                            station.getLocation().getLatitude(), station.getLocation().getLongitude());
                    if (dist > MAX_SEARCH_RADIUS_KM) return null;

                    int onDutyCrew = (int) officerShiftRepository
                            .findByStationAndDate(station.getUid(), today)
                            .stream()
                            .filter(s -> isOnDuty(s.getStartTime(), s.getEndTime(), now))
                            .count();

                    long activeIncidents = countPoliceActiveIncidents(station.getUid());
                    int eta = (int) Math.ceil((dist / POLICE_SPEED_KMH) * 60.0);
                    int score = computeScore(dist, (int) activeIncidents, onDutyCrew, 0);

                    List<String> warnings = new ArrayList<>();
                    if (onDutyCrew == 0) warnings.add("No officers currently on shift");
                    if (activeIncidents >= 5) warnings.add("High workload: " + activeIncidents + " active incidents");

                    return SmartDispatchRecommendation.UnitRecommendation.builder()
                            .uid(station.getUid())
                            .name(station.getName())
                            .unitType("POLICE_STATION")
                            .resourceType("POLICE_STATION")
                            .agencyName(station.getAgency() != null ? station.getAgency().getName() : null)
                            .distanceKm(Math.round(dist * 10.0) / 10.0)
                            .etaMinutes(eta)
                            .onDutyCrewCount(onDutyCrew)
                            .activeIncidentCount((int) activeIncidents)
                            .availableVehicles(0)
                            .score(score)
                            .unitLat(station.getLocation().getLatitude())
                            .unitLng(station.getLocation().getLongitude())
                            .warnings(warnings)
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(SmartDispatchRecommendation.UnitRecommendation::getScore).reversed())
                .limit(TOP_N)
                .collect(Collectors.toList());
    }

    // ── Fire ──────────────────────────────────────────────────────────────────

    /**
     * Routing: OperationalPost(FIRE_OPERATIONAL_POST) first, EmergencyUnit(FIRE_*) fallback.
     */
    private List<SmartDispatchRecommendation.UnitRecommendation> buildFireRecommendations(
            double lat, double lng) {

        List<SmartDispatchRecommendation.UnitRecommendation> posts =
                buildPostRecommendations(lat, lng, PostType.FIRE_OPERATIONAL_POST, FIRE_SPEED_KMH);
        if (posts.size() >= TOP_N) return posts;

        List<SmartDispatchRecommendation.UnitRecommendation> units = Stream.of(
                        UnitType.FIRE_BRIGADE, UnitType.FIRE_DISTRICT_STATION,
                        UnitType.FIRE_REGIONAL_COMMAND, UnitType.AIRPORT_FIRE_UNIT)
                .flatMap(ut -> buildUnitRecommendations(lat, lng, ut, FIRE_SPEED_KMH).stream())
                .sorted(Comparator.comparingInt(SmartDispatchRecommendation.UnitRecommendation::getScore).reversed())
                .collect(Collectors.toList());

        return mergeAndRank(posts, units);
    }

    // ── Medical ───────────────────────────────────────────────────────────────

    /**
     * Routing: OperationalPost(MEDICAL_OPERATIONAL_POST) first, EmergencyUnit(HOSPITAL_AMBULANCE_UNIT) fallback.
     */
    private List<SmartDispatchRecommendation.UnitRecommendation> buildMedicalRecommendations(
            double lat, double lng) {

        List<SmartDispatchRecommendation.UnitRecommendation> posts =
                buildPostRecommendations(lat, lng, PostType.MEDICAL_OPERATIONAL_POST, MEDICAL_SPEED_KMH);
        if (posts.size() >= TOP_N) return posts;

        List<SmartDispatchRecommendation.UnitRecommendation> units =
                buildUnitRecommendations(lat, lng, UnitType.HOSPITAL_AMBULANCE_UNIT, MEDICAL_SPEED_KMH);

        return mergeAndRank(posts, units);
    }

    // ── Operational Post scoring ──────────────────────────────────────────────

    private List<SmartDispatchRecommendation.UnitRecommendation> buildPostRecommendations(
            double lat, double lng, PostType postType, double speedKmh) {

        return operationalPostRepository.findNearby(lat, lng, MAX_SEARCH_RADIUS_KM, postType.name()).stream()
                .map(post -> {
                    if (post.getLocation() == null
                            || post.getLocation().getLatitude() == null
                            || post.getLocation().getLongitude() == null) return null;

                    double dist = haversine(lat, lng,
                            post.getLocation().getLatitude(), post.getLocation().getLongitude());

                    int onDutyCrew = countOnDutyAtPost(post);
                    int availableVehicles = countAvailableVehiclesAtPost(post);
                    long activeIncidents = 0; // Posts are small — no incident counter yet

                    List<String> warnings = new ArrayList<>();
                    if (onDutyCrew == 0) warnings.add("No crew currently on shift at this post");
                    if (availableVehicles == 0) warnings.add("No vehicles available at this post");

                    int eta = (int) Math.ceil((dist / speedKmh) * 60.0);
                    // Posts get a 10-point bonus for proximity — they are purpose-deployed
                    int score = Math.min(100, computeScore(dist, (int) activeIncidents, onDutyCrew, availableVehicles) + 10);

                    return SmartDispatchRecommendation.UnitRecommendation.builder()
                            .uid(post.getUid())
                            .name(post.getName())
                            .unitType(post.getPostType().name())
                            .resourceType("OPERATIONAL_POST")
                            .postUid(post.getUid())
                            .agencyName(post.getAgency() != null ? post.getAgency().getName() : null)
                            .distanceKm(Math.round(dist * 10.0) / 10.0)
                            .etaMinutes(eta)
                            .onDutyCrewCount(onDutyCrew)
                            .activeIncidentCount(0)
                            .availableVehicles(availableVehicles)
                            .score(score)
                            .unitLat(post.getLocation().getLatitude())
                            .unitLng(post.getLocation().getLongitude())
                            .warnings(warnings)
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(SmartDispatchRecommendation.UnitRecommendation::getScore).reversed())
                .limit(TOP_N)
                .collect(Collectors.toList());
    }

    // ── EmergencyUnit scoring (fire & medical stations) ───────────────────────

    private List<SmartDispatchRecommendation.UnitRecommendation> buildUnitRecommendations(
            double lat, double lng, UnitType unitType, double speedKmh) {

        return emergencyUnitRepository.findByUnitTypeAndIsActiveTrue(unitType).stream()
                .filter(u -> u.getLocation() != null
                        && u.getLocation().getLatitude() != null
                        && u.getLocation().getLongitude() != null)
                .map(unit -> {
                    double dist = haversine(lat, lng,
                            unit.getLocation().getLatitude(), unit.getLocation().getLongitude());
                    if (dist > MAX_SEARCH_RADIUS_KM) return null;

                    int onDutyCrew = countOnDutyForUnit(unit, unitType);
                    int activeVehicles = vehicleRepository.findByUnitUidAndStatus(unit.getUid(), VehicleStatus.AVAILABLE).size();

                    long activeIncidents = Optional.ofNullable(
                            incidentRepository.countByUnitAndStatus(unit.getUid(), IncidentStatus.IN_PROGRESS))
                            .orElse(0L)
                            + Optional.ofNullable(
                            incidentRepository.countByUnitAndStatus(unit.getUid(), IncidentStatus.DISPATCHED))
                            .orElse(0L);

                    int eta = (int) Math.ceil((dist / speedKmh) * 60.0);
                    int score = computeScore(dist, (int) activeIncidents, onDutyCrew, activeVehicles);

                    List<String> warnings = new ArrayList<>();
                    if (onDutyCrew == 0) warnings.add("No crew currently on duty");
                    if (activeVehicles == 0) warnings.add("No vehicles available");
                    if (activeIncidents >= 3) warnings.add("High workload: " + activeIncidents + " active incidents");

                    return SmartDispatchRecommendation.UnitRecommendation.builder()
                            .uid(unit.getUid())
                            .name(unit.getName())
                            .unitType(unit.getUnitType() != null ? unit.getUnitType().name() : null)
                            .resourceType("EMERGENCY_UNIT")
                            .agencyName(unit.getAgency() != null ? unit.getAgency().getName() : null)
                            .distanceKm(Math.round(dist * 10.0) / 10.0)
                            .etaMinutes(eta)
                            .onDutyCrewCount(onDutyCrew)
                            .activeIncidentCount((int) activeIncidents)
                            .availableVehicles(activeVehicles)
                            .score(score)
                            .unitLat(unit.getLocation().getLatitude())
                            .unitLng(unit.getLocation().getLongitude())
                            .warnings(warnings)
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(SmartDispatchRecommendation.UnitRecommendation::getScore).reversed())
                .limit(TOP_N)
                .collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<SmartDispatchRecommendation.UnitRecommendation> mergeAndRank(
            List<SmartDispatchRecommendation.UnitRecommendation> primary,
            List<SmartDispatchRecommendation.UnitRecommendation> secondary) {

        return Stream.concat(primary.stream(), secondary.stream())
                .sorted(Comparator.comparingInt(SmartDispatchRecommendation.UnitRecommendation::getScore).reversed())
                .limit(TOP_N)
                .collect(Collectors.toList());
    }

    private int countOnDutyAtPost(OperationalPost post) {
        // Count officer shifts + vehicle shifts deployed at this post today
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        long officerCount = officerShiftRepository.findAll().stream()
                .filter(s -> s.getOperationalPost() != null
                        && post.getUid().equals(s.getOperationalPost().getUid())
                        && today.equals(s.getShiftDate())
                        && isOnDuty(s.getStartTime(), s.getEndTime(), now)
                        && Boolean.TRUE.equals(s.getIsActive()))
                .count();

        return (int) officerCount;
    }

    private int countAvailableVehiclesAtPost(OperationalPost post) {
        return (int) vehicleRepository.findAll().stream()
                .filter(v -> v.getStatus() == VehicleStatus.AVAILABLE && Boolean.TRUE.equals(v.getIsActive()))
                .count(); // Will be scoped to post once vehicle→post relationship is added
    }

    private int countOnDutyForUnit(EmergencyUnit unit, UnitType type) {
        if (type == UnitType.FIRE_BRIGADE || type.name().startsWith("FIRE") || type == UnitType.AIRPORT_FIRE_UNIT) {
            return (int) fireOfficerRepository.findAll().stream()
                    .filter(f -> Boolean.TRUE.equals(f.getIsOnDuty()) && Boolean.TRUE.equals(f.getIsActive())
                            && f.getEmergencyUnit() != null && unit.getUid().equals(f.getEmergencyUnit().getUid()))
                    .count();
        }
        return (int) medicRepository.findAll().stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsOnDuty()) && Boolean.TRUE.equals(m.getIsActive())
                        && m.getEmergencyUnit() != null && unit.getUid().equals(m.getEmergencyUnit().getUid()))
                .count();
    }

    private long countPoliceActiveIncidents(String stationUid) {
        return Optional.ofNullable(
                incidentRepository.countByPoliceStationAndStatus(stationUid, IncidentStatus.IN_PROGRESS))
                .orElse(0L)
                + Optional.ofNullable(
                incidentRepository.countByPoliceStationAndStatus(stationUid, IncidentStatus.DISPATCHED))
                .orElse(0L);
    }

    private boolean isTrafficIncident(IncidentType type) {
        return type == IncidentType.ACCIDENT || type == IncidentType.TRAFFIC_VIOLATION;
    }

    private boolean isCrimeIncident(IncidentType type) {
        return type == IncidentType.THEFT || type == IncidentType.ROBBERY
                || type == IncidentType.ASSAULT || type == IncidentType.BURGLARY
                || type == IncidentType.MURDER || type == IncidentType.KIDNAPPING;
    }

    private boolean hasLocation(com.smartincident.incidentbackend.police.entity.Location loc) {
        return loc != null && loc.getLatitude() != null && loc.getLongitude() != null;
    }

    private int computeScore(double distKm, int activeIncidents, int onDutyCrew, int availableVehicles) {
        double distScore     = 100.0 * Math.max(0, 1.0 - distKm / MAX_SEARCH_RADIUS_KM);
        double workloadScore = 100.0 * Math.max(0, 1.0 - activeIncidents / 10.0);
        double crewScore     = Math.min(100.0, onDutyCrew * 15.0);
        double vehicleScore  = Math.min(100.0, availableVehicles * 25.0);
        double composite     = 0.40 * distScore + 0.30 * workloadScore + 0.20 * crewScore + 0.10 * vehicleScore;
        return (int) Math.round(composite);
    }

    private boolean isOnDuty(LocalTime start, LocalTime end, LocalTime now) {
        if (end.isAfter(start)) return !now.isBefore(start) && !now.isAfter(end);
        return !now.isBefore(start) || !now.isAfter(end);
    }

    private double estimateVehicleSpeed(VehicleType type) {
        return switch (type) {
            case AMBULANCE, ADVANCED_AMBULANCE -> MEDICAL_SPEED_KMH;
            case FIRE_TRUCK, LADDER_TRUCK, WATER_TENDER, RESCUE_TRUCK -> FIRE_SPEED_KMH;
            default -> POLICE_SPEED_KMH;
        };
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
