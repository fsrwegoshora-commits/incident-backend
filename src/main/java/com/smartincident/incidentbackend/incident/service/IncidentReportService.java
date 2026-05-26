package com.smartincident.incidentbackend.incident.service;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.dispatcher.repository.DispatcherShiftRepository;
import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.emergency.repository.EmergencyUnitRepository;
import com.smartincident.incidentbackend.enums.*;
import com.smartincident.incidentbackend.incident.dto.IncidentReportDto;
import com.smartincident.incidentbackend.incident.entity.IncidentAgency;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.incident.repository.IncidentAgencyRepository;
import com.smartincident.incidentbackend.incident.repository.IncidentReportRepository;
import com.smartincident.incidentbackend.notification.dto.NotificationDto;
import com.smartincident.incidentbackend.notification.service.NotificationService;
import com.smartincident.incidentbackend.police.entity.PoliceOfficer;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
import com.smartincident.incidentbackend.police.repository.PoliceOfficerRepository;
import com.smartincident.incidentbackend.police.repository.PoliceStationRepository;
import com.smartincident.incidentbackend.setting.entity.Agency;
import com.smartincident.incidentbackend.setting.repository.AgencyRepository;
import com.smartincident.incidentbackend.utils.*;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@GraphQLApi
public class IncidentReportService {

    private final IncidentReportRepository incidentRepository;
    private final UserRepository userRepository;
    private final EmergencyUnitRepository emergencyUnitRepository;
    private final PoliceOfficerRepository officerRepository;
    private final PoliceStationRepository policeStationRepository;
    private final AgencyRepository agencyRepository;
    private final IncidentAgencyRepository incidentAgencyRepository;
    private final NotificationService notificationService;
    private final DispatcherShiftRepository dispatcherShiftRepository;

    // ── Create ─────────────────────────────────────────────────────────────

    @Transactional
    public Response<IncidentReport> createIncident(IncidentReportDto dto) {
        log.info("Creating incident report: {}", dto.getTitle());

        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty())
            return Response.error("Title is required");
        if (dto.getType() == null)
            return Response.error("Incident type is required");
        if (dto.getLocation() == null || dto.getLocation().trim().isEmpty())
            return Response.error("Location is required");
        if (dto.getLatitude() == null || dto.getLongitude() == null)
            return Response.error("GPS coordinates are required");
        if (dto.getEmergencyCategory() == null)
            return Response.error("Emergency category is required");

        String loggedUserUid = LoggedUser.getUid();
        if (loggedUserUid == null)
            return Response.error("User not authenticated");

        Optional<User> reporterOpt = userRepository.findByUid(loggedUserUid);
        if (reporterOpt.isEmpty())
            return Response.error("Reporter not found");
        User reporter = reporterOpt.get();

        // Derive service flags from category
        EmergencyCategory category = dto.getEmergencyCategory();
        boolean needsPolice  = needsPolice(category);
        boolean needsFire    = needsFire(category);
        boolean needsMedical = needsMedical(category);

        IncidentReport incident = new IncidentReport();
        incident.setTitle(dto.getTitle().trim());
        incident.setDescription(dto.getDescription());
        incident.setType(dto.getType());
        incident.setLocation(dto.getLocation());
        incident.setLatitude(dto.getLatitude());
        incident.setLongitude(dto.getLongitude());
        incident.setImageUrl(dto.getImageUrl());
        incident.setAudioUrl(dto.getAudioUrl());
        incident.setVideoUrl(dto.getVideoUrl());
        incident.setSymptoms(dto.getSymptoms());
        incident.setIsLiveCallRequested(Boolean.TRUE.equals(dto.getIsLiveCallRequested()));
        incident.setStatus(IncidentStatus.REPORTED);
        incident.setReportedBy(reporter);
        incident.setEmergencyCategory(category);
        incident.setRequiresPoliceService(needsPolice);
        incident.setRequiresFireService(needsFire);
        incident.setRequiresMedicalService(needsMedical);
        incident.setIsMajorIncident(Boolean.TRUE.equals(dto.getIsMajorIncident()));
        incident.setEocActivated(Boolean.TRUE.equals(dto.getEocActivated()));
        incident.setEmergencyLevel(dto.getEmergencyLevel() != null ? dto.getEmergencyLevel() : EmergencyLevel.MEDIUM);
        incident.setReportedAt(LocalDateTime.now());

        // ── Police assignment ──────────────────────────────────────────────
        if (needsPolice) {
            PoliceStation station = null;
            if (dto.getAssignedPoliceStationUid() != null) {
                station = policeStationRepository.findByUid(dto.getAssignedPoliceStationUid())
                        .orElse(null);
            }
            if (station == null) {
                station = findNearestPoliceStation(dto.getLatitude(), dto.getLongitude());
            }
            if (station == null)
                return Response.error("No police station found near your location");
            incident.setAssignedPoliceStation(station);
            log.info("Assigned police station: {}", station.getName());
        }

        // ── Fire / Medical assignment ──────────────────────────────────────
        if (needsFire || needsMedical) {
            EmergencyUnit unit = null;
            if (dto.getAssignedUnitUid() != null) {
                unit = emergencyUnitRepository.findByUid(dto.getAssignedUnitUid()).orElse(null);
            }
            if (unit == null) {
                UnitType preferredType = needsMedical
                        ? UnitType.HOSPITAL_AMBULANCE_UNIT
                        : UnitType.FIRE_BRIGADE;
                unit = findNearestEmergencyUnit(dto.getLatitude(), dto.getLongitude(), preferredType);
            }
            if (unit == null) {
                String svcName = needsMedical ? "medical (ambulance)" : "fire";
                return Response.error("No " + svcName + " unit found near your location");
            }
            incident.setAssignedUnit(unit);
            log.info("Assigned emergency unit: {}", unit.getName());
        }

        // Lead agency = first in priority order: Medical > Police > Fire
        resolveLeadAgency(incident, needsPolice, needsFire, needsMedical);

        // ── Auto-assign to Dispatcher on duty ──────────────────────────────
        assignOnDutyDispatcher(incident);

        // Optional explicit officer assignment (police incidents only)
        if (dto.getAssignedOfficerUid() != null && needsPolice) {
            Optional<PoliceOfficer> officerOpt = officerRepository.findByUid(dto.getAssignedOfficerUid());
            if (officerOpt.isEmpty())
                return Response.error("Assigned officer not found");
            PoliceOfficer officer = officerOpt.get();
            if (incident.getAssignedPoliceStation() != null &&
                !officer.getPoliceStation().getUid().equals(incident.getAssignedPoliceStation().getUid()))
                return Response.error("Officer does not belong to the assigned police station");
            incident.setAssignedOfficer(officer);
        }

        try {
            IncidentReport saved = incidentRepository.save(incident);
            log.info("Incident created: {}", saved.getUid());

            // Create IncidentAgency records + notify each involved agency
            createAgencyRecordsAndNotify(saved, needsPolice, needsFire, needsMedical);

            return new Response<>(saved);
        } catch (Exception e) {
            log.error("Failed to create incident", e);
            throw e;
        }
    }

    // ── Update ─────────────────────────────────────────────────────────────

    @Transactional
    public Response<IncidentReport> updateIncident(IncidentReportDto dto) {
        log.info("Updating incident: {}", dto.getUid());

        if (dto.getUid() == null)
            return Response.error("Incident UID is required");

        Optional<IncidentReport> incidentOpt = incidentRepository.findByUid(dto.getUid());
        if (incidentOpt.isEmpty())
            return Response.error("Incident not found");

        IncidentReport incident = incidentOpt.get();

        if (dto.getTitle() != null) incident.setTitle(dto.getTitle());
        if (dto.getDescription() != null) incident.setDescription(dto.getDescription());
        if (dto.getSymptoms() != null) incident.setSymptoms(dto.getSymptoms());
        if (dto.getStatus() != null) {
            incident.setStatus(dto.getStatus());
            if (dto.getStatus() == IncidentStatus.RESOLVED)
                incident.setResolvedAt(LocalDateTime.now());
        }
        if (dto.getRequiresFireService() != null)
            incident.setRequiresFireService(dto.getRequiresFireService());
        if (dto.getRequiresMedicalService() != null)
            incident.setRequiresMedicalService(dto.getRequiresMedicalService());
        if (dto.getRequiresPoliceService() != null)
            incident.setRequiresPoliceService(dto.getRequiresPoliceService());

        if (dto.getAssignedOfficerUid() != null) {
            officerRepository.findByUid(dto.getAssignedOfficerUid())
                    .ifPresent(incident::setAssignedOfficer);
        }

        incident.update();

        try {
            incident = incidentRepository.save(incident);
            log.info("Incident updated: {}", incident.getUid());
            return new Response<>(incident);
        } catch (Exception e) {
            log.error("Failed to update incident: {}", e.getMessage());
            return Response.error("Failed to update incident: " + Utils.getExceptionMessage(e));
        }
    }

    // ── Assign officer ──────────────────────────────────────────────────────

    @Transactional
    public Response<IncidentReport> assignOfficer(String incidentUid, String officerUid) {
        log.info("Assigning officer {} to incident {}", officerUid, incidentUid);

        Optional<IncidentReport> incidentOpt = incidentRepository.findByUid(incidentUid);
        if (incidentOpt.isEmpty()) return Response.error("Incident not found");

        Optional<PoliceOfficer> officerOpt = officerRepository.findByUid(officerUid);
        if (officerOpt.isEmpty()) return Response.error("Officer not found");

        IncidentReport incident = incidentOpt.get();
        PoliceOfficer officer = officerOpt.get();

        incident.setAssignedOfficer(officer);
        incident.setStatus(IncidentStatus.DISPATCHED);
        incident.update();

        try {
            incident = incidentRepository.save(incident);
            log.info("Officer assigned successfully");
            notifyOfficerAssignment(incident, officer);
            return new Response<>(incident + "Officer assigned successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("Failed to assign officer: " + Utils.getExceptionMessage(e));
        }
    }

    // ── Read ───────────────────────────────────────────────────────────────

    public Response<IncidentReport> getIncident(String uid) {
        if (uid == null) return Response.error("Incident UID is required");
        return incidentRepository.findByUid(uid)
                .map(Response::success)
                .orElseGet(() -> Response.error("Incident not found"));
    }

    public ResponsePage<IncidentReport> getMyIncidents(PageableParam pageableParam) {
        String userUid = LoggedUser.getUid();
        if (userUid == null) return new ResponsePage<>("User not authenticated");
        Page<IncidentReport> incidents = incidentRepository.findByReporter(
                userUid, pageableParam.getIsActive(), pageableParam.key(), pageableParam.getPageable(true));
        return new ResponsePage<>(incidents);
    }

    /**
     * Returns incidents for the currently logged-in station/unit admin.
     * Handles both Police (uses policeStation) and Fire/Medical (uses emergencyUnit).
     */
    public ResponsePage<IncidentReport> getStationIncidents(PageableParam pageableParam, IncidentStatus status) {
        Role role = LoggedUser.getRole();

        // Police admins/officers use policeStation
        if (role == Role.STATION_ADMIN || role == Role.POLICE_OFFICER) {
            String stationUid = LoggedUser.getPoliceStationUid();
            if (stationUid == null) return new ResponsePage<>("Police station not found for user");
            return new ResponsePage<>(incidentRepository.findByPoliceStation(
                    stationUid, status, pageableParam.getIsActive(),
                    pageableParam.key(), pageableParam.getPageable(true)));
        }

        // Fire/Medical admins use emergencyUnit
        String unitUid = LoggedUser.getEmergencyUnitUid();
        if (unitUid == null) return new ResponsePage<>("Emergency unit not found for user");
        return new ResponsePage<>(incidentRepository.findByEmergencyUnit(
                unitUid, status, pageableParam.getIsActive(),
                pageableParam.key(), pageableParam.getPageable(true)));
    }

    /** Pending incidents at the logged-in user's station — used by the dispatcher assignment panel. */
    public ResponsePage<IncidentReport> getPendingStationIncidents(PageableParam pageableParam) {
        String stationUid = LoggedUser.getPoliceStationUid();
        if (stationUid == null) {
            Page<IncidentReport> all = incidentRepository.findAll(pageableParam.getPageable(true));
            Page<IncidentReport> pending = new org.springframework.data.domain.PageImpl<>(
                all.stream().filter(i -> isNewOrPending(i.getStatus()) && i.getIsActive()).toList(),
                pageableParam.getPageable(true), all.getTotalElements());
            return new ResponsePage<>(pending);
        }
        // Fetch all for station (null status = all) and filter for new/pending lifecycle states
        Page<IncidentReport> all = incidentRepository.findByPoliceStation(
                stationUid, null, true, pageableParam.key(), pageableParam.getPageable(true));
        Page<IncidentReport> pending = new org.springframework.data.domain.PageImpl<>(
            all.stream().filter(i -> isNewOrPending(i.getStatus())).toList(),
            pageableParam.getPageable(true), all.getTotalElements());
        return new ResponsePage<>(pending);
    }

    private boolean isNewOrPending(IncidentStatus status) {
        return status == IncidentStatus.REPORTED || status == IncidentStatus.PENDING
                || status == IncidentStatus.UNDER_REVIEW || status == IncidentStatus.CLASSIFIED
                || status == IncidentStatus.WAITING_FOR_DISPATCH;
    }

    public ResponsePage<IncidentReport> getOfficerIncidents(PageableParam pageableParam, IncidentStatus status) {
        String officerUid = LoggedUser.getOfficerUid();
        if (officerUid == null) return new ResponsePage<>("Officer not found");
        Page<IncidentReport> incidents = incidentRepository.findByOfficer(
                officerUid, status, pageableParam.getIsActive(),
                pageableParam.key(), pageableParam.getPageable(true));
        return new ResponsePage<>(incidents);
    }

    public Response<List<IncidentReport>> getNearbyIncidents(
            Double latitude, Double longitude, Double radiusKm, IncidentStatus status) {
        if (latitude == null || longitude == null)
            return Response.error("Coordinates are required");
        if (radiusKm == null || radiusKm <= 0) radiusKm = 10.0;
        List<IncidentReport> incidents = incidentRepository.findNearbyIncidents(
                latitude, longitude, radiusKm, status != null ? status.name() : null);
        return Response.success(incidents);
    }

    // ── Delete (soft) ──────────────────────────────────────────────────────

    @Transactional
    public Response<IncidentReport> deleteIncident(String uid) {
        if (uid == null) return Response.error("Incident UID is required");
        Optional<IncidentReport> incidentOpt = incidentRepository.findByUid(uid);
        if (incidentOpt.isEmpty()) return Response.error("Incident not found");
        IncidentReport incident = incidentOpt.get();
        if (!incident.getIsActive()) return Response.error("Incident already deleted");
        incident.delete();
        try {
            incidentRepository.save(incident);
            log.info("Incident deleted: {}", uid);
            return new Response<>(incident + "Incident deleted successfully");
        } catch (Exception e) {
            log.error("Failed to delete incident: {}", e.getMessage());
            return Response.error("Failed to delete incident: " + Utils.getExceptionMessage(e));
        }
    }

    // ── Dispatch Queue ─────────────────────────────────────────────────────

    /** Returns incidents in the dispatch queue — scoped by role. */
    public ResponsePage<IncidentReport> getDispatchQueue(PageableParam pageableParam, IncidentStatus status) {
        Role role = LoggedUser.getRole();
        String userUid = LoggedUser.getUid();
        if (userUid == null) return new ResponsePage<>("User not authenticated");

        // DISPATCH_CENTER_ADMIN and DISPATCHER_SUPERVISOR see all incidents for their center
        if (role == Role.DISPATCH_CENTER_ADMIN || role == Role.DISPATCHER_SUPERVISOR) {
            String centerUid = LoggedUser.getEmergencyUnitUid();
            if (centerUid == null) return new ResponsePage<>("No dispatch center assigned");
            return new ResponsePage<>(incidentRepository.findDispatchQueueByCenter(
                    centerUid, status, pageableParam.key(), pageableParam.getPageable(true)));
        }

        // ROOT sees all incidents regardless of dispatcher assignment
        if (role == Role.ROOT) {
            Page<IncidentReport> all = incidentRepository.findAll(pageableParam.getPageable(true));
            List<IncidentReport> filtered = all.stream()
                    .filter(i -> i.getIsActive() && (status == null || i.getStatus() == status))
                    .toList();
            return new ResponsePage<>(new org.springframework.data.domain.PageImpl<>(
                    filtered, pageableParam.getPageable(true), filtered.size()));
        }

        // DISPATCHER sees only their own assigned incidents
        return new ResponsePage<>(incidentRepository.findDispatchQueue(
                userUid, status, pageableParam.key(), pageableParam.getPageable(true)));
    }

    /** All pre-dispatch incidents across every dispatcher queue — for ROOT/AGENCY_ADMIN oversight. */
    public ResponsePage<IncidentReport> getAllPendingIncidents(PageableParam pageableParam) {
        Page<IncidentReport> page = incidentRepository.findAll(pageableParam.getPageable(true));
        List<IncidentReport> pending = page.stream()
                .filter(i -> isNewOrPending(i.getStatus()) && i.getIsActive())
                .toList();
        return new ResponsePage<>(new org.springframework.data.domain.PageImpl<>(
                pending, pageableParam.getPageable(true), pending.size()));
    }

    // ── Statistics ─────────────────────────────────────────────────────────

    public Response<IncidentStats> getStationStats(String stationUid) {
        Role role = LoggedUser.getRole();

        if (stationUid == null) {
            stationUid = (role == Role.STATION_ADMIN || role == Role.POLICE_OFFICER)
                    ? LoggedUser.getPoliceStationUid()
                    : LoggedUser.getEmergencyUnitUid();
        }
        if (stationUid == null) return Response.error("Station/unit not found");

        IncidentStats stats = new IncidentStats();
        boolean isPolice = (role == Role.STATION_ADMIN || role == Role.POLICE_OFFICER);

        if (isPolice) {
            stats.setPending(incidentRepository.countByPoliceStationAndStatus(stationUid, IncidentStatus.REPORTED)
                           + incidentRepository.countByPoliceStationAndStatus(stationUid, IncidentStatus.PENDING));
            stats.setInProgress(incidentRepository.countByPoliceStationAndStatus(stationUid, IncidentStatus.IN_PROGRESS));
            stats.setResolved(incidentRepository.countByPoliceStationAndStatus(stationUid, IncidentStatus.RESOLVED));
        } else {
            stats.setPending(incidentRepository.countByUnitAndStatus(stationUid, IncidentStatus.REPORTED)
                           + incidentRepository.countByUnitAndStatus(stationUid, IncidentStatus.PENDING));
            stats.setInProgress(incidentRepository.countByUnitAndStatus(stationUid, IncidentStatus.IN_PROGRESS));
            stats.setResolved(incidentRepository.countByUnitAndStatus(stationUid, IncidentStatus.RESOLVED));
        }

        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        List<IncidentReport> recentIncidents = incidentRepository.findRecentIncidents(last24Hours, stationUid);
        stats.setRecentCount((long) recentIncidents.size());

        return Response.success(stats);
    }

    // ── Private helpers ────────────────────────────────────────────────────

    // ── Status transition state machine ───────────────────────────────────────

    private static final java.util.EnumMap<IncidentStatus, java.util.EnumSet<IncidentStatus>> TRANSITIONS;
    static {
        TRANSITIONS = new java.util.EnumMap<>(IncidentStatus.class);
        TRANSITIONS.put(IncidentStatus.PENDING,               java.util.EnumSet.of(IncidentStatus.UNDER_REVIEW, IncidentStatus.REPORTED, IncidentStatus.REJECTED));
        TRANSITIONS.put(IncidentStatus.REPORTED,              java.util.EnumSet.of(IncidentStatus.UNDER_REVIEW, IncidentStatus.REJECTED));
        TRANSITIONS.put(IncidentStatus.UNDER_REVIEW,          java.util.EnumSet.of(IncidentStatus.CLASSIFIED, IncidentStatus.REPORTED, IncidentStatus.REJECTED));
        TRANSITIONS.put(IncidentStatus.CLASSIFIED,            java.util.EnumSet.of(IncidentStatus.WAITING_FOR_DISPATCH, IncidentStatus.UNDER_REVIEW));
        TRANSITIONS.put(IncidentStatus.WAITING_FOR_DISPATCH,  java.util.EnumSet.of(IncidentStatus.DISPATCHED, IncidentStatus.CLASSIFIED));
        TRANSITIONS.put(IncidentStatus.DISPATCHED,            java.util.EnumSet.of(IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS));
        TRANSITIONS.put(IncidentStatus.ACKNOWLEDGED,          java.util.EnumSet.of(IncidentStatus.EN_ROUTE));
        TRANSITIONS.put(IncidentStatus.EN_ROUTE,              java.util.EnumSet.of(IncidentStatus.AT_SCENE));
        TRANSITIONS.put(IncidentStatus.AT_SCENE,              java.util.EnumSet.of(IncidentStatus.IN_PROGRESS));
        TRANSITIONS.put(IncidentStatus.IN_PROGRESS,           java.util.EnumSet.of(IncidentStatus.RESOLVED));
        TRANSITIONS.put(IncidentStatus.RESOLVED,              java.util.EnumSet.of(IncidentStatus.CLOSED));
    }

    @Transactional
    public Response<IncidentReport> transitionStatus(String incidentUid, IncidentStatus newStatus) {
        if (incidentUid == null) return Response.error("Incident UID is required");
        if (newStatus == null) return Response.error("Target status is required");

        Optional<IncidentReport> opt = incidentRepository.findByUid(incidentUid);
        if (opt.isEmpty()) return Response.error("Incident not found");
        IncidentReport incident = opt.get();

        IncidentStatus current = incident.getStatus();
        java.util.Set<IncidentStatus> allowed = TRANSITIONS.getOrDefault(current, java.util.EnumSet.noneOf(IncidentStatus.class));
        if (!allowed.contains(newStatus))
            return Response.error("Cannot transition from " + current + " to " + newStatus
                    + ". Allowed: " + allowed);

        incident.setStatus(newStatus);
        if (newStatus == IncidentStatus.DISPATCHED) incident.setDispatchedAt(LocalDateTime.now());
        if (newStatus == IncidentStatus.AT_SCENE)   incident.setAtSceneAt(LocalDateTime.now());
        if (newStatus == IncidentStatus.RESOLVED)   incident.setResolvedAt(LocalDateTime.now());
        incident.update();

        try {
            incident = incidentRepository.save(incident);
            notifyStatusTransition(incident, newStatus);
            log.info("Incident {} transitioned: {} → {}", incidentUid, current, newStatus);
            return Response.success(incident);
        } catch (Exception e) {
            log.error("Failed to transition incident status: {}", e.getMessage());
            return Response.error("Failed to update incident status");
        }
    }

    private void notifyStatusTransition(IncidentReport incident, IncidentStatus newStatus) {
        if (incident.getAssignedDispatcher() == null) return;
        boolean shouldNotify = newStatus == IncidentStatus.ACKNOWLEDGED
                || newStatus == IncidentStatus.AT_SCENE
                || newStatus == IncidentStatus.RESOLVED;
        if (!shouldNotify) return;

        String statusLabel = newStatus.name().replace('_', ' ');
        NotificationDto dto = new NotificationDto();
        dto.setTitle("Incident Update: " + statusLabel);
        dto.setMessage("\"" + incident.getTitle() + "\" is now " + statusLabel);
        dto.setType(NotificationType.INCIDENT_ASSIGNED);
        dto.setChannels(List.of(NotificationChannel.IN_APP));
        dto.setRelatedEntityUid(incident.getUid());
        dto.setRelatedEntityType("INCIDENT");
        dto.setTargetUserUids(List.of(incident.getAssignedDispatcher().getUid()));
        notificationService.sendNotification(dto);
    }

    // ── Dispatcher load balancing ─────────────────────────────────────────────

    private void assignOnDutyDispatcher(IncidentReport incident) {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalTime now = java.time.LocalTime.now();
        java.util.List<com.smartincident.incidentbackend.dispatcher.entity.DispatcherShift> onDuty =
                dispatcherShiftRepository.findOnDutyNow(today, now);
        if (onDuty.isEmpty()) {
            log.warn("No dispatcher on duty — incident {} will have no assigned dispatcher", incident.getTitle());
            return;
        }
        // Least-active load balancing: assign to dispatcher with fewest unresolved incidents.
        // DISPATCHER role is preferred over SUPERVISOR (bias of +1000 in sort key).
        User dispatcher = onDuty.stream()
                .map(s -> s.getDispatcher())
                .min(java.util.Comparator.comparingLong(u -> {
                    Long active = incidentRepository.countActiveIncidentsByDispatcher(u.getUid());
                    long roleWeight = u.getRole() == Role.DISPATCHER ? 0L : 1000L;
                    return (active != null ? active : 0L) + roleWeight;
                }))
                .orElseGet(() -> onDuty.get(0).getDispatcher());
        incident.setAssignedDispatcher(dispatcher);
        log.info("Incident auto-assigned to dispatcher: {} (least-active load balancing)", dispatcher.getName());
    }

    private boolean needsPolice(EmergencyCategory cat) {
        return cat == EmergencyCategory.POLICE_ONLY || cat == EmergencyCategory.POLICE_MEDICAL
                || cat == EmergencyCategory.POLICE_FIRE || cat == EmergencyCategory.ALL_THREE;
    }

    private boolean needsFire(EmergencyCategory cat) {
        return cat == EmergencyCategory.FIRE_ONLY || cat == EmergencyCategory.POLICE_FIRE
                || cat == EmergencyCategory.MEDICAL_FIRE || cat == EmergencyCategory.ALL_THREE;
    }

    private boolean needsMedical(EmergencyCategory cat) {
        return cat == EmergencyCategory.MEDICAL_ONLY || cat == EmergencyCategory.POLICE_MEDICAL
                || cat == EmergencyCategory.MEDICAL_FIRE || cat == EmergencyCategory.ALL_THREE;
    }

    private PoliceStation findNearestPoliceStation(double lat, double lng) {
        List<PoliceStation> all = policeStationRepository.findByIsActiveTrue();
        return all.stream()
                .filter(s -> s.getLocation() != null
                        && s.getLocation().getLatitude() != null
                        && s.getLocation().getLongitude() != null)
                .min(Comparator.comparingDouble(s -> haversine(lat, lng,
                        s.getLocation().getLatitude(), s.getLocation().getLongitude())))
                .orElse(null);
    }

    private EmergencyUnit findNearestEmergencyUnit(double lat, double lng, UnitType preferredType) {
        List<EmergencyUnit> candidates = emergencyUnitRepository.findByUnitTypeAndIsActiveTrue(preferredType);
        if (candidates.isEmpty()) {
            // Try broader search if preferred type has no units yet
            candidates = emergencyUnitRepository.findAll().stream()
                    .filter(u -> u.getIsActive() && u.getLocation() != null)
                    .toList();
        }
        return candidates.stream()
                .filter(u -> u.getLocation() != null
                        && u.getLocation().getLatitude() != null
                        && u.getLocation().getLongitude() != null)
                .min(Comparator.comparingDouble(u -> haversine(lat, lng,
                        u.getLocation().getLatitude(), u.getLocation().getLongitude())))
                .orElse(null);
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

    private void resolveLeadAgency(IncidentReport incident,
                                   boolean needsPolice, boolean needsFire, boolean needsMedical) {
        String code = needsMedical ? "MEDICAL" : needsPolice ? "POLICE" : "FIRE";
        agencyRepository.findByCode(code)
                .ifPresent(obj -> incident.setLeadAgency((Agency) obj));
    }

    private void createAgencyRecordsAndNotify(IncidentReport incident,
                                               boolean police, boolean fire, boolean medical) {
        if (police) {
            createAgencyRecord(incident, "POLICE", AgencyRole.LEAD);
            notifyPoliceStation(incident);
        }
        if (medical) {
            createAgencyRecord(incident, "MEDICAL", police ? AgencyRole.SUPPORT : AgencyRole.LEAD);
            notifyEmergencyUnit(incident, "MEDICAL");
        }
        if (fire) {
            AgencyRole role = (!police && !medical) ? AgencyRole.LEAD : AgencyRole.SUPPORT;
            createAgencyRecord(incident, "FIRE", role);
            notifyEmergencyUnit(incident, "FIRE");
        }
        notifyAllDispatchers(incident);
        notifyAssignedDispatcher(incident);
    }

    private void notifyAllDispatchers(IncidentReport incident) {
        List<String> dispatcherUids = userRepository.findByRoleInAndIsActiveTrue(
                        List.of(Role.DISPATCHER, Role.DISPATCHER_SUPERVISOR, Role.DISPATCH_CENTER_ADMIN))
                .stream().map(User::getUid).toList();
        if (dispatcherUids.isEmpty()) return;
        NotificationDto dto = new NotificationDto();
        dto.setTitle("New Incident Requires Dispatch");
        dto.setMessage("A new " + incident.getEmergencyLevel().name() + " incident has been reported: " + incident.getTitle());
        dto.setType(NotificationType.INCIDENT_REPORTED);
        dto.setChannels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH));
        dto.setRelatedEntityUid(incident.getUid());
        dto.setRelatedEntityType("INCIDENT");
        dto.setTargetUserUids(dispatcherUids);
        notificationService.sendNotification(dto);
    }

    private void createAgencyRecord(IncidentReport incident, String agencyCode, AgencyRole role) {
        agencyRepository.findByCode(agencyCode).ifPresent(obj -> {
            Agency agency = (Agency) obj;
            IncidentAgency ia = new IncidentAgency();
            ia.setIncident(incident);
            ia.setAgency(agency);
            ia.setAgencyRole(role);
            ia.setResponseStatus(AgencyResponseStatus.NOTIFIED);
            ia.setNotifiedAt(LocalDateTime.now());
            incidentAgencyRepository.save(ia);
        });
    }

    private void notifyPoliceStation(IncidentReport incident) {
        if (incident.getAssignedPoliceStation() == null) return;
        NotificationDto dto = new NotificationDto();
        dto.setTitle("New Incident Reported");
        dto.setMessage("Police response required: " + incident.getTitle());
        dto.setType(NotificationType.INCIDENT_REPORTED);
        dto.setChannels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH));
        dto.setRelatedEntityUid(incident.getUid());
        dto.setRelatedEntityType("INCIDENT");
        dto.setTargetRole(Role.STATION_ADMIN);
        dto.setTargetStationUid(incident.getAssignedPoliceStation().getUid());
        notificationService.sendNotificationByRoleAndPoliceStation(dto);
    }

    private void notifyEmergencyUnit(IncidentReport incident, String agencyCode) {
        if (incident.getAssignedUnit() == null) return;
        Role targetRole = Role.STATION_ADMIN;
        NotificationDto dto = new NotificationDto();
        dto.setTitle("New Incident — " + agencyCode + " Response Required");
        dto.setMessage("Incident reported: " + incident.getTitle());
        dto.setType(NotificationType.INCIDENT_REPORTED);
        dto.setChannels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH));
        dto.setRelatedEntityUid(incident.getUid());
        dto.setRelatedEntityType("INCIDENT");
        dto.setTargetRole(targetRole);
        dto.setTargetStationUid(incident.getAssignedUnit().getUid());
        notificationService.sendNotificationByRoleAndStation(dto);
    }

    private void notifyAssignedDispatcher(IncidentReport incident) {
        if (incident.getAssignedDispatcher() == null) return;
        NotificationDto dto = new NotificationDto();
        dto.setTitle("Incident Assigned To Your Queue");
        dto.setMessage("New incident in your dispatch queue: " + incident.getTitle()
                + " [" + incident.getEmergencyLevel().name() + "]");
        dto.setType(NotificationType.INCIDENT_REPORTED);
        dto.setChannels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH));
        dto.setRelatedEntityUid(incident.getUid());
        dto.setRelatedEntityType("INCIDENT");
        dto.setTargetUserUids(List.of(incident.getAssignedDispatcher().getUid()));
        notificationService.sendNotification(dto);
    }

    private void notifyOfficerAssignment(IncidentReport incident, PoliceOfficer officer) {
        NotificationDto dto = new NotificationDto();
        dto.setTitle("Incident Assigned To You");
        dto.setMessage("You have been assigned to incident: " + incident.getTitle());
        dto.setType(NotificationType.INCIDENT_ASSIGNED);
        dto.setChannels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH));
        dto.setRelatedEntityUid(incident.getUid());
        dto.setRelatedEntityType("INCIDENT");
        dto.setTargetUserUids(List.of(officer.getUserAccount().getUid()));
        notificationService.sendNotification(dto);
    }

    // ── Escalation ───────────────────────────────────────────────────────────

    @Transactional
    public Response<IncidentReport> escalateIncident(String incidentUid, String note) {
        if (incidentUid == null) return Response.error("Incident UID is required");

        Optional<IncidentReport> opt = incidentRepository.findByUid(incidentUid);
        if (opt.isEmpty()) return Response.error("Incident not found");
        IncidentReport incident = opt.get();

        String callerUid = LoggedUser.getUid();
        User caller = callerUid != null ? userRepository.findByUid(callerUid).orElse(null) : null;

        incident.setEscalatedAt(LocalDateTime.now());
        incident.setEscalationNote(note);
        if (caller != null) incident.setEscalatedBy(caller);
        incident.update();

        try {
            incident = incidentRepository.save(incident);
            notifyEscalation(incident, caller);
            log.info("Incident {} escalated by {}", incidentUid, callerUid);
            return Response.success(incident);
        } catch (Exception e) {
            log.error("Failed to escalate incident: {}", e.getMessage());
            return Response.error("Failed to escalate incident");
        }
    }

    private void notifyEscalation(IncidentReport incident, User escalatedBy) {
        List<String> supervisorUids = userRepository.findByRoleInAndIsActiveTrue(
                        List.of(Role.DISPATCHER_SUPERVISOR, Role.DISPATCH_CENTER_ADMIN))
                .stream().map(User::getUid).toList();
        if (supervisorUids.isEmpty()) return;
        String escalatorName = escalatedBy != null ? escalatedBy.getName() : "A dispatcher";
        NotificationDto dto = new NotificationDto();
        dto.setTitle("Incident Escalated — Supervisor Action Required");
        dto.setMessage(escalatorName + " escalated incident \"" + incident.getTitle()
                + "\": " + (incident.getEscalationNote() != null ? incident.getEscalationNote() : "Resources unavailable"));
        dto.setType(NotificationType.INCIDENT_ASSIGNED);
        dto.setChannels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH));
        dto.setRelatedEntityUid(incident.getUid());
        dto.setRelatedEntityType("INCIDENT");
        dto.setTargetUserUids(supervisorUids);
        notificationService.sendNotification(dto);
    }

    // ── Inner class ─────────────────────────────────────────────────────────

    @lombok.Data
    public static class IncidentStats {
        private Long pending;
        private Long inProgress;
        private Long resolved;
        private Long recentCount;
    }
}
