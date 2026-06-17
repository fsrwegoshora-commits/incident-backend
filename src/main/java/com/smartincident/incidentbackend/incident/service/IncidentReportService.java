package com.smartincident.incidentbackend.incident.service;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.dispatcher.repository.DispatcherShiftRepository;
import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.emergency.repository.EmergencyUnitRepository;
import com.smartincident.incidentbackend.classification.service.AiTriageService;
import com.smartincident.incidentbackend.enums.*;
import com.smartincident.incidentbackend.incident.dto.CommandStructureDto;
import com.smartincident.incidentbackend.incident.dto.CorrelationResult;
import com.smartincident.incidentbackend.incident.dto.DesignateCommanderRequest;
import com.smartincident.incidentbackend.incident.dto.IncidentReportDto;
import com.smartincident.incidentbackend.incident.dto.IncidentTimelineDto;
import com.smartincident.incidentbackend.incident.entity.IncidentAgency;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.incident.entity.IncidentStatusHistory;
import com.smartincident.incidentbackend.incident.repository.IncidentAgencyRepository;
import com.smartincident.incidentbackend.incident.repository.IncidentReportRepository;
import com.smartincident.incidentbackend.incident.repository.IncidentStatusHistoryRepository;
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
    private final IncidentStatusHistoryRepository statusHistoryRepository;
    private final IncidentCorrelationService correlationService;
    private final AiTriageService aiTriageService;

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
        EmergencyLevel level = dto.getEmergencyLevel() != null ? dto.getEmergencyLevel() : EmergencyLevel.MEDIUM;
        incident.setEmergencyLevel(level);
        incident.setReportedAt(LocalDateTime.now());
        incident.setSlaDeadline(LocalDateTime.now().plusMinutes(slaMinutesForLevel(level)));

        // ── AI Triage — classify EMERGENCY vs NON_EMERGENCY ───────────────
        try {
            AiTriageService.TriageResult triage = aiTriageService.triage(dto.getTitle(), dto.getDescription());
            incident.setNature(triage.nature());
            incident.setDetectedLanguage(triage.language());
            incident.setTriageReasoning(triage.reasoning());
            log.info("Triage: nature={} lang={} confidence={} — {}",
                    triage.nature(), triage.language(), triage.confidence(), triage.reasoning());
        } catch (Exception e) {
            log.warn("Triage failed, defaulting to EMERGENCY: {}", e.getMessage());
            incident.setNature(IncidentNature.EMERGENCY);
        }

        // ── Duplicate / Correlation Detection ─────────────────────────────
        CorrelationResult correlation = correlationService.detect(dto);

        if (correlation.isDuplicate()) {
            log.info("Duplicate incident suppressed: reporter={} master={}",
                     reporter.getUid(), correlation.getMasterIncident().getUid());
            return correlationService.linkDuplicate(correlation, reporter, dto);
        }

        // Lead agency = first in priority order: Medical > Police > Fire
        resolveLeadAgency(incident, needsPolice, needsFire, needsMedical);

        // ── Auto-assign to Dispatcher — only for EMERGENCY incidents ───────
        if (incident.getNature() == IncidentNature.EMERGENCY) {
            assignOnDutyDispatcher(incident);
        }

        try {
            IncidentReport saved = incidentRepository.save(incident);
            log.info("Incident created: {} nature={}", saved.getUid(), saved.getNature());

            if (saved.getNature() == IncidentNature.EMERGENCY) {
                // EMERGENCY → notify dispatchers + agencies
                createAgencyRecordsAndNotify(saved, needsPolice, needsFire, needsMedical);
            } else {
                // NON_EMERGENCY → skip dispatcher queue; notify assigned police station directly
                resolveNearestPoliceStation(saved);
                if (saved.getAssignedPoliceStation() != null) {
                    notifyPoliceStation(saved);
                }
            }

            // If a RELATED incident was detected, record the cross-link
            if (correlation.isRelated()) {
                correlationService.recordRelated(saved, correlation);
                log.info("Related incident link recorded: new={} related={}",
                         saved.getUid(), correlation.getMasterIncident().getUid());
            }

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

        if (incident.getAssignedPoliceStation() != null
                && !officer.getPoliceStation().getUid().equals(incident.getAssignedPoliceStation().getUid()))
            return Response.error("Officer does not belong to the assigned police station");

        incident.setAssignedOfficer(officer);
        incident.update();

        try {
            incident = incidentRepository.save(incident);
            log.info("Officer assigned: {} → incident {}", officer.getBadgeNumber(), incidentUid);
            notifyOfficerAssignment(incident, officer);

            // Advance to DISPATCHED through the state machine if allowed from current status
            if (TRANSITIONS.getOrDefault(incident.getStatus(), java.util.EnumSet.noneOf(IncidentStatus.class))
                    .contains(IncidentStatus.DISPATCHED)) {
                transitionStatus(incidentUid, IncidentStatus.DISPATCHED, "Officer assigned");
                return incidentRepository.findByUid(incidentUid).map(Response::success)
                        .orElseGet(() -> Response.error("Incident not found after dispatch"));
            }

            return Response.success(incident);
        } catch (Exception e) {
            log.error("Failed to assign officer: {}", e.getMessage());
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
     * Investigation queue — NON_EMERGENCY incidents assigned to the logged-in station.
     * Bypasses the dispatcher and is managed directly by the station admin.
     */
    public ResponsePage<IncidentReport> getInvestigationQueue(PageableParam pageableParam, IncidentStatus status) {
        String stationUid = LoggedUser.getPoliceStationUid();
        if (stationUid == null) return new ResponsePage<>("Police station not found for user");
        return new ResponsePage<>(incidentRepository.findInvestigationQueue(
                stationUid, IncidentNature.NON_EMERGENCY, status,
                pageableParam.getIsActive(), pageableParam.key(), pageableParam.getPageable(true)));
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
            return new ResponsePage<>(incidentRepository.findAllPendingIncidents(pageableParam.getPageable(true)));
        }
        return new ResponsePage<>(incidentRepository.findPendingByPoliceStation(
                stationUid, pageableParam.getPageable(true)));
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
            return new ResponsePage<>(incidentRepository.findAllActiveIncidents(
                    status, pageableParam.getPageable(true)));
        }

        // DISPATCHER sees only their own assigned incidents
        return new ResponsePage<>(incidentRepository.findDispatchQueue(
                userUid, status, pageableParam.key(), pageableParam.getPageable(true)));
    }

    /** All pre-dispatch incidents across every dispatcher queue — for ROOT/AGENCY_ADMIN oversight. */
    public ResponsePage<IncidentReport> getAllPendingIncidents(PageableParam pageableParam) {
        return new ResponsePage<>(incidentRepository.findAllPendingIncidents(pageableParam.getPageable(true)));
    }

    // ── SLA queries ────────────────────────────────────────────────────────

    public ResponsePage<IncidentReport> getSlaBreachedIncidents(PageableParam pageableParam) {
        return new ResponsePage<>(incidentRepository.findSlaBreached(pageableParam.getPageable(true)));
    }

    public ResponsePage<IncidentReport> getSlaApproachingIncidents(PageableParam pageableParam, int withinMinutes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusMinutes(withinMinutes);
        return new ResponsePage<>(incidentRepository.findSlaApproaching(now, threshold, pageableParam.getPageable(true)));
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

    /** Roles permitted to trigger each target status. */
    private static final java.util.EnumMap<IncidentStatus, java.util.EnumSet<Role>> TRANSITION_ROLES;

    static {
        TRANSITIONS = new java.util.EnumMap<>(IncidentStatus.class);
        TRANSITIONS.put(IncidentStatus.PENDING,               java.util.EnumSet.of(IncidentStatus.UNDER_REVIEW, IncidentStatus.REPORTED, IncidentStatus.REJECTED));
        TRANSITIONS.put(IncidentStatus.REPORTED,              java.util.EnumSet.of(IncidentStatus.UNDER_REVIEW, IncidentStatus.CLASSIFIED, IncidentStatus.REJECTED));
        TRANSITIONS.put(IncidentStatus.UNDER_REVIEW,          java.util.EnumSet.of(IncidentStatus.CLASSIFIED, IncidentStatus.REPORTED, IncidentStatus.REJECTED));
        TRANSITIONS.put(IncidentStatus.CLASSIFIED,            java.util.EnumSet.of(IncidentStatus.WAITING_FOR_DISPATCH, IncidentStatus.UNDER_REVIEW));
        TRANSITIONS.put(IncidentStatus.WAITING_FOR_DISPATCH,  java.util.EnumSet.of(IncidentStatus.DISPATCHED, IncidentStatus.CLASSIFIED));
        TRANSITIONS.put(IncidentStatus.DISPATCHED,            java.util.EnumSet.of(IncidentStatus.ACKNOWLEDGED, IncidentStatus.IN_PROGRESS));
        TRANSITIONS.put(IncidentStatus.ACKNOWLEDGED,          java.util.EnumSet.of(IncidentStatus.EN_ROUTE));
        TRANSITIONS.put(IncidentStatus.EN_ROUTE,              java.util.EnumSet.of(IncidentStatus.AT_SCENE));
        TRANSITIONS.put(IncidentStatus.AT_SCENE,              java.util.EnumSet.of(IncidentStatus.IN_PROGRESS));
        TRANSITIONS.put(IncidentStatus.IN_PROGRESS,           java.util.EnumSet.of(IncidentStatus.RESOLVED));
        TRANSITIONS.put(IncidentStatus.RESOLVED,              java.util.EnumSet.of(IncidentStatus.CLOSED));

        // Roles that may drive each target status
        TRANSITION_ROLES = new java.util.EnumMap<>(IncidentStatus.class);
        java.util.EnumSet<Role> dispatchRoles = java.util.EnumSet.of(
                Role.DISPATCHER, Role.DISPATCHER_SUPERVISOR, Role.DISPATCH_CENTER_ADMIN, Role.ROOT);
        java.util.EnumSet<Role> fieldRoles = java.util.EnumSet.of(
                Role.POLICE_OFFICER, Role.FIRE_OFFICER, Role.MEDIC,
                Role.DISPATCHER, Role.DISPATCHER_SUPERVISOR, Role.DISPATCH_CENTER_ADMIN, Role.ROOT);
        java.util.EnumSet<Role> allStaffRoles = java.util.EnumSet.of(
                Role.DISPATCHER, Role.DISPATCHER_SUPERVISOR, Role.DISPATCH_CENTER_ADMIN,
                Role.POLICE_OFFICER, Role.FIRE_OFFICER, Role.MEDIC,
                Role.STATION_ADMIN, Role.AGENCY_ADMIN, Role.ROOT);

        TRANSITION_ROLES.put(IncidentStatus.REPORTED,              allStaffRoles);
        TRANSITION_ROLES.put(IncidentStatus.UNDER_REVIEW,          dispatchRoles);
        TRANSITION_ROLES.put(IncidentStatus.CLASSIFIED,            dispatchRoles);
        TRANSITION_ROLES.put(IncidentStatus.WAITING_FOR_DISPATCH,  dispatchRoles);
        TRANSITION_ROLES.put(IncidentStatus.DISPATCHED,            dispatchRoles);
        TRANSITION_ROLES.put(IncidentStatus.REJECTED,              dispatchRoles);
        TRANSITION_ROLES.put(IncidentStatus.ACKNOWLEDGED,          fieldRoles);
        TRANSITION_ROLES.put(IncidentStatus.EN_ROUTE,              fieldRoles);
        TRANSITION_ROLES.put(IncidentStatus.AT_SCENE,              fieldRoles);
        TRANSITION_ROLES.put(IncidentStatus.IN_PROGRESS,           fieldRoles);
        TRANSITION_ROLES.put(IncidentStatus.RESOLVED,              fieldRoles);
        TRANSITION_ROLES.put(IncidentStatus.CLOSED,                dispatchRoles);
    }

    @Transactional
    public Response<IncidentReport> transitionStatus(String incidentUid, IncidentStatus newStatus) {
        return transitionStatus(incidentUid, newStatus, null);
    }

    @Transactional
    public Response<IncidentReport> transitionStatus(String incidentUid, IncidentStatus newStatus, String note) {
        if (incidentUid == null) return Response.error("Incident UID is required");
        if (newStatus == null)   return Response.error("Target status is required");

        Optional<IncidentReport> opt = incidentRepository.findByUid(incidentUid);
        if (opt.isEmpty()) return Response.error("Incident not found");
        IncidentReport incident = opt.get();

        IncidentStatus current = incident.getStatus();

        // Guard: valid transition?
        java.util.Set<IncidentStatus> allowed = TRANSITIONS.getOrDefault(current, java.util.EnumSet.noneOf(IncidentStatus.class));
        if (!allowed.contains(newStatus))
            return Response.error("Cannot transition from " + current + " to " + newStatus + ". Allowed: " + allowed);

        // Guard: caller role authorised for this target status?
        Role callerRole = LoggedUser.getRole();
        java.util.Set<Role> permittedRoles = TRANSITION_ROLES.getOrDefault(newStatus, java.util.EnumSet.noneOf(Role.class));
        if (callerRole != null && !permittedRoles.contains(callerRole))
            return Response.error("Your role (" + callerRole + ") is not authorised to set status " + newStatus);

        incident.setStatus(newStatus);
        LocalDateTime now = LocalDateTime.now();
        if (newStatus == IncidentStatus.DISPATCHED)   incident.setDispatchedAt(now);
        if (newStatus == IncidentStatus.ACKNOWLEDGED) incident.setAcknowledgedAt(now);
        if (newStatus == IncidentStatus.EN_ROUTE)     incident.setEnRouteAt(now);
        if (newStatus == IncidentStatus.AT_SCENE)     incident.setAtSceneAt(now);
        if (newStatus == IncidentStatus.RESOLVED)     incident.setResolvedAt(now);
        if (newStatus == IncidentStatus.CLOSED)       incident.setClosedAt(now);
        incident.update();

        // Persist history record
        IncidentStatusHistory history = IncidentStatusHistory.builder()
                .incident(incident)
                .fromStatus(current)
                .toStatus(newStatus)
                .changedByUid(LoggedUser.getUid())
                .changedByName(LoggedUser.getName())
                .changedByRole(callerRole != null ? callerRole.name() : null)
                .changedAt(now)
                .note(note)
                .build();

        try {
            incident = incidentRepository.save(incident);
            statusHistoryRepository.save(history);
            notifyStatusTransition(incident, newStatus);
            log.info("Incident {} transitioned: {} → {} by {}", incidentUid, current, newStatus, LoggedUser.getUid());
            return Response.success(incident);
        } catch (Exception e) {
            log.error("Failed to transition incident status: {}", e.getMessage());
            return Response.error("Failed to update incident status");
        }
    }

    public Response<List<IncidentStatusHistory>> getStatusHistory(String incidentUid) {
        if (incidentUid == null) return Response.error("Incident UID is required");
        if (incidentRepository.findByUid(incidentUid).isEmpty())
            return Response.error("Incident not found");
        return Response.success(statusHistoryRepository.findByIncidentUid(incidentUid));
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

    // ── Dispatcher load balancing with fallback ───────────────────────────────

    private void assignOnDutyDispatcher(IncidentReport incident) {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalTime now = java.time.LocalTime.now();
        java.util.List<com.smartincident.incidentbackend.dispatcher.entity.DispatcherShift> onDuty =
                dispatcherShiftRepository.findOnDutyNow(today, now);

        if (!onDuty.isEmpty()) {
            // Least-active load balancing — DISPATCHER preferred over SUPERVISOR (bias +1000)
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
            return;
        }

        // Fallback 1: any DISPATCHER_SUPERVISOR active in the system
        log.warn("No dispatcher on duty — falling back to DISPATCHER_SUPERVISOR for incident: {}", incident.getTitle());
        java.util.List<User> supervisors = userRepository.findByRoleInAndIsActiveTrue(
                java.util.List.of(Role.DISPATCHER_SUPERVISOR));
        if (!supervisors.isEmpty()) {
            User supervisor = supervisors.stream()
                    .min(java.util.Comparator.comparingLong(u -> {
                        Long active = incidentRepository.countActiveIncidentsByDispatcher(u.getUid());
                        return active != null ? active : 0L;
                    }))
                    .orElse(supervisors.get(0));
            incident.setAssignedDispatcher(supervisor);
            log.warn("Incident {} assigned to supervisor fallback: {}", incident.getTitle(), supervisor.getName());
            notifySupervisorFallback(incident, supervisor);
            return;
        }

        // Fallback 2: any DISPATCH_CENTER_ADMIN
        log.warn("No supervisor available — falling back to DISPATCH_CENTER_ADMIN for incident: {}", incident.getTitle());
        java.util.List<User> dcAdmins = userRepository.findByRoleInAndIsActiveTrue(
                java.util.List.of(Role.DISPATCH_CENTER_ADMIN));
        if (!dcAdmins.isEmpty()) {
            incident.setAssignedDispatcher(dcAdmins.get(0));
            log.warn("Incident {} assigned to DC Admin fallback: {}", incident.getTitle(), dcAdmins.get(0).getName());
            notifySupervisorFallback(incident, dcAdmins.get(0));
            return;
        }

        log.error("CRITICAL: No dispatcher, supervisor, or DC admin available for incident: {}", incident.getTitle());
        notifyNoDispatcherAvailable(incident);
    }

    private void notifySupervisorFallback(IncidentReport incident, User fallback) {
        NotificationDto dto = new NotificationDto();
        dto.setTitle("Dispatcher Fallback Alert");
        dto.setMessage("No dispatcher on duty. Incident \"" + incident.getTitle()
                + "\" assigned to " + fallback.getRole().name() + " " + fallback.getName()
                + " as fallback.");
        dto.setType(NotificationType.INCIDENT_ASSIGNED);
        dto.setChannels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH));
        dto.setRelatedEntityUid(incident.getUid());
        dto.setRelatedEntityType("INCIDENT");
        dto.setTargetUserUids(List.of(fallback.getUid()));
        notificationService.sendNotification(dto);
    }

    private void notifyNoDispatcherAvailable(IncidentReport incident) {
        List<String> adminUids = userRepository.findByRoleInAndIsActiveTrue(
                        List.of(Role.ROOT, Role.AGENCY_ADMIN))
                .stream().map(User::getUid).toList();
        if (adminUids.isEmpty()) return;
        NotificationDto dto = new NotificationDto();
        dto.setTitle("CRITICAL: No Dispatcher Available");
        dto.setMessage("Incident \"" + incident.getTitle()
                + "\" has no dispatcher assigned — no dispatcher, supervisor, or DC admin is active.");
        dto.setType(NotificationType.INCIDENT_ASSIGNED);
        dto.setChannels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH));
        dto.setRelatedEntityUid(incident.getUid());
        dto.setRelatedEntityType("INCIDENT");
        dto.setTargetUserUids(adminUids);
        notificationService.sendNotification(dto);
    }

    private long slaMinutesForLevel(EmergencyLevel level) {
        return switch (level) {
            case CRITICAL -> 5;
            case HIGH     -> 10;
            case MEDIUM   -> 20;
            case LOW      -> 60;
        };
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

    /**
     * For NON_EMERGENCY incidents: assign to the nearest active police station.
     * Uses simple proximity based on incident GPS coordinates; falls back to any active station.
     */
    private void resolveNearestPoliceStation(IncidentReport incident) {
        if (incident.getAssignedPoliceStation() != null) return; // already set

        try {
            var stations = policeStationRepository.findAll().stream()
                    .filter(s -> s.getIsActive() != null && s.getIsActive())
                    .toList();
            if (stations.isEmpty()) return;

            if (incident.getLatitude() != null && incident.getLongitude() != null) {
                double lat = incident.getLatitude();
                double lon = incident.getLongitude();
                var nearest = stations.stream()
                        .filter(s -> s.getLocation() != null && s.getLocation().getLatitude() != null)
                        .min(java.util.Comparator.comparingDouble(s -> {
                            double dLat = s.getLocation().getLatitude()  - lat;
                            double dLon = s.getLocation().getLongitude() - lon;
                            return dLat * dLat + dLon * dLon;
                        }))
                        .orElse(stations.get(0));
                incident.setAssignedPoliceStation(nearest);
            } else {
                incident.setAssignedPoliceStation(stations.get(0));
            }
            log.info("NON_EMERGENCY incident {} assigned to station {}",
                    incident.getUid(), incident.getAssignedPoliceStation().getName());
        } catch (Exception e) {
            log.warn("Could not resolve nearest police station: {}", e.getMessage());
        }
    }

    private void resolveLeadAgency(IncidentReport incident,
                                   boolean needsPolice, boolean needsFire, boolean needsMedical) {
        String code = needsMedical ? "MEDICAL" : needsPolice ? "POLICE" : "FIRE";
        agencyRepository.findByCode(code)
                .ifPresent(obj -> incident.setLeadAgency((Agency) obj));
    }

    private void createAgencyRecordsAndNotify(IncidentReport incident,
                                               boolean police, boolean fire, boolean medical) {
        if (police) createAgencyRecord(incident, "POLICE", AgencyRole.LEAD);
        if (medical) createAgencyRecord(incident, "MEDICAL", police ? AgencyRole.SUPPORT : AgencyRole.LEAD);
        if (fire)    createAgencyRecord(incident, "FIRE", (!police && !medical) ? AgencyRole.LEAD : AgencyRole.SUPPORT);
        // Stations and units are notified at dispatch time (not at report time).
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

    // ── Closure Review Workflow (V2) ─────────────────────────────────────────

    @Transactional
    public Response<IncidentReport> stationReview(String incidentUid, String notes) {
        Optional<IncidentReport> opt = incidentRepository.findByUid(incidentUid);
        if (opt.isEmpty()) return Response.error("Incident not found");
        IncidentReport incident = opt.get();

        if (incident.getStatus() != IncidentStatus.RESOLVED)
            return Response.error("Station review is only available for RESOLVED incidents");

        User reviewer = userRepository.findByUid(LoggedUser.getUid()).orElse(null);
        incident.setStationReviewedBy(reviewer);
        incident.setStationReviewedAt(LocalDateTime.now());
        incident.setStationReviewNotes(notes);
        incident.update();
        IncidentReport saved = incidentRepository.save(incident);

        // Notify dispatcher that station review is complete
        NotificationDto dto = new NotificationDto();
        dto.setTitle("Station Review Complete");
        dto.setMessage("Incident \"" + incident.getTitle() + "\" has been reviewed by the station. Dispatcher sign-off required.");
        dto.setType(NotificationType.INCIDENT_UPDATE);
        dto.setChannels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH));
        dto.setRelatedEntityUid(incidentUid);
        dto.setRelatedEntityType("INCIDENT");
        dto.setTargetRole(Role.DISPATCHER);
        if (incident.getAssignedDispatcher() != null)
            dto.setTargetUserUids(List.of(incident.getAssignedDispatcher().getUid()));
        try { notificationService.sendNotification(dto); } catch (Exception ignored) {}

        return Response.success(saved);
    }

    @Transactional
    public Response<IncidentReport> dispatcherReview(String incidentUid, String notes) {
        Optional<IncidentReport> opt = incidentRepository.findByUid(incidentUid);
        if (opt.isEmpty()) return Response.error("Incident not found");
        IncidentReport incident = opt.get();

        if (incident.getStatus() != IncidentStatus.RESOLVED)
            return Response.error("Dispatcher review is only available for RESOLVED incidents");
        if (incident.getStationReviewedAt() == null)
            return Response.error("Station review must be completed before dispatcher review");

        User reviewer = userRepository.findByUid(LoggedUser.getUid()).orElse(null);
        incident.setDispatcherReviewedBy(reviewer);
        incident.setDispatcherReviewedAt(LocalDateTime.now());
        incident.setDispatcherReviewNotes(notes);
        incident.update();
        return Response.success(incidentRepository.save(incident));
    }

    // ── Incident Commander Designation (Critical 4) ───────────────────────────

    @Transactional
    public Response<IncidentReport> designateCommander(String incidentUid, DesignateCommanderRequest req) {
        if (incidentUid == null) return Response.error("Incident UID is required");
        if (req == null || req.getCommanderUserUid() == null)
            return Response.error("Commander user UID is required");

        Optional<IncidentReport> opt = incidentRepository.findByUid(incidentUid);
        if (opt.isEmpty()) return Response.error("Incident not found");
        IncidentReport incident = opt.get();

        Optional<User> commanderOpt = userRepository.findByUid(req.getCommanderUserUid());
        if (commanderOpt.isEmpty()) return Response.error("Commander user not found");
        User commander = commanderOpt.get();

        incident.setIncidentCommander(commander);
        if (req.getLeadAgencyUid() != null) {
            agencyRepository.findByUid(req.getLeadAgencyUid())
                    .ifPresent(obj -> incident.setIncidentCommanderAgency((Agency) obj));
        } else {
            incident.setIncidentCommanderAgency(commander.getAgency());
        }
        incident.update();

        try {
            IncidentReport saved = incidentRepository.save(incident);
            notifyCommanderDesignation(saved, commander);
            log.info("Commander {} designated for incident {}", commander.getName(), incidentUid);
            return Response.success(saved);
        } catch (Exception e) {
            log.error("Failed to designate commander: {}", e.getMessage());
            return Response.error("Failed to designate commander");
        }
    }

    public Response<CommandStructureDto> getCommandStructure(String incidentUid) {
        if (incidentUid == null) return Response.error("Incident UID is required");
        Optional<IncidentReport> opt = incidentRepository.findByUid(incidentUid);
        if (opt.isEmpty()) return Response.error("Incident not found");
        IncidentReport incident = opt.get();

        List<IncidentAgency> agencies = incidentAgencyRepository.findByIncidentUid(incidentUid);

        List<CommandStructureDto.AgencySummary> agencySummaries = agencies.stream()
                .map(ia -> CommandStructureDto.AgencySummary.builder()
                        .agencyUid(ia.getAgency().getUid())
                        .agencyName(ia.getAgency().getName())
                        .agencyType(ia.getAgency().getAgencyType())
                        .role(ia.getAgencyRole() != null ? ia.getAgencyRole().name() : null)
                        .responseStatus(ia.getResponseStatus() != null ? ia.getResponseStatus().name() : null)
                        .notifiedAt(ia.getNotifiedAt())
                        .respondedAt(ia.getRespondedAt())
                        .build())
                .toList();

        CommandStructureDto dto = CommandStructureDto.builder()
                .incidentUid(incident.getUid())
                .incidentTitle(incident.getTitle())
                .commanderUid(incident.getIncidentCommander() != null ? incident.getIncidentCommander().getUid() : null)
                .commanderName(incident.getIncidentCommander() != null ? incident.getIncidentCommander().getName() : null)
                .commanderRole(incident.getIncidentCommander() != null ? incident.getIncidentCommander().getRole().name() : null)
                .commanderPhone(incident.getIncidentCommander() != null ? incident.getIncidentCommander().getPhoneNumber() : null)
                .leadAgencyUid(incident.getIncidentCommanderAgency() != null ? incident.getIncidentCommanderAgency().getUid() : null)
                .leadAgencyName(incident.getIncidentCommanderAgency() != null ? incident.getIncidentCommanderAgency().getName() : null)
                .leadAgencyType(incident.getIncidentCommanderAgency() != null ? incident.getIncidentCommanderAgency().getAgencyType() : null)
                .eocActivated(incident.getEocActivated())
                .isMajorIncident(incident.getIsMajorIncident())
                .involvedAgencies(agencySummaries)
                .build();

        return Response.success(dto);
    }

    private void notifyCommanderDesignation(IncidentReport incident, User commander) {
        List<IncidentAgency> agencies = incidentAgencyRepository.findByIncidentUid(incident.getUid());
        java.util.Set<String> involvedAgencyUids = agencies.stream()
                .map(ia -> ia.getAgency().getUid()).collect(java.util.stream.Collectors.toSet());
        List<String> agencyAdminUids = userRepository.findByRoleInAndIsActiveTrue(List.of(Role.AGENCY_ADMIN))
                .stream()
                .filter(u -> u.getAgency() != null && involvedAgencyUids.contains(u.getAgency().getUid()))
                .map(User::getUid)
                .toList();
        if (agencyAdminUids.isEmpty()) return;
        NotificationDto dto = new NotificationDto();
        dto.setTitle("Incident Commander Designated");
        dto.setMessage(commander.getName() + " has been designated as Incident Commander for: " + incident.getTitle());
        dto.setType(NotificationType.INCIDENT_ASSIGNED);
        dto.setChannels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH));
        dto.setRelatedEntityUid(incident.getUid());
        dto.setRelatedEntityType("INCIDENT");
        dto.setTargetUserUids(agencyAdminUids);
        notificationService.sendNotification(dto);
    }

    // ── Citizen Incident Timeline (High 1) ────────────────────────────────────

    public Response<IncidentTimelineDto> getIncidentTimeline(String incidentUid) {
        if (incidentUid == null) return Response.error("Incident UID is required");
        Optional<IncidentReport> opt = incidentRepository.findByUid(incidentUid);
        if (opt.isEmpty()) return Response.error("Incident not found");
        IncidentReport incident = opt.get();

        List<IncidentStatusHistory> history = statusHistoryRepository.findByIncidentUid(incidentUid);

        List<IncidentTimelineDto.TimelineEvent> events = history.stream()
                .map(h -> IncidentTimelineDto.TimelineEvent.builder()
                        .timestamp(h.getChangedAt())
                        .eventType("STATUS_CHANGE")
                        .description(formatStatusChange(h.getFromStatus(), h.getToStatus()))
                        .actorName(h.getChangedByName())
                        .actorRole(h.getChangedByRole())
                        .fromStatus(h.getFromStatus() != null ? h.getFromStatus().name() : null)
                        .toStatus(h.getToStatus() != null ? h.getToStatus().name() : null)
                        .note(h.getNote())
                        .build())
                .sorted(java.util.Comparator.comparing(IncidentTimelineDto.TimelineEvent::getTimestamp))
                .toList();

        IncidentTimelineDto dto = IncidentTimelineDto.builder()
                .incidentUid(incident.getUid())
                .title(incident.getTitle())
                .type(incident.getType() != null ? incident.getType().name() : null)
                .location(incident.getLocation())
                .currentStatus(incident.getStatus().name())
                .emergencyLevel(incident.getEmergencyLevel() != null ? incident.getEmergencyLevel().name() : null)
                .emergencyCategory(incident.getEmergencyCategory() != null ? incident.getEmergencyCategory().name() : null)
                .assignedPoliceStation(incident.getAssignedPoliceStation() != null ? incident.getAssignedPoliceStation().getName() : null)
                .assignedUnit(incident.getAssignedUnit() != null ? incident.getAssignedUnit().getName() : null)
                .assignedOfficer(incident.getAssignedOfficer() != null ? incident.getAssignedOfficer().getBadgeNumber() : null)
                .assignedDispatcher(incident.getAssignedDispatcher() != null ? incident.getAssignedDispatcher().getName() : null)
                .reportedAt(incident.getReportedAt())
                .dispatchedAt(incident.getDispatchedAt())
                .acknowledgedAt(incident.getAcknowledgedAt())
                .enRouteAt(incident.getEnRouteAt())
                .atSceneAt(incident.getAtSceneAt())
                .resolvedAt(incident.getResolvedAt())
                .closedAt(incident.getClosedAt())
                .slaDeadline(incident.getSlaDeadline())
                .slaBreached(incident.getSlaBreachedAt() != null)
                .events(events)
                .build();

        return Response.success(dto);
    }

    private String formatStatusChange(IncidentStatus from, IncidentStatus to) {
        String fromLabel = from != null ? from.name().replace('_', ' ') : "INITIAL";
        String toLabel   = to   != null ? to.name().replace('_', ' ')   : "UNKNOWN";
        return switch (to) {
            case REPORTED            -> "Incident reported and logged";
            case UNDER_REVIEW        -> "Incident is under review by dispatch";
            case CLASSIFIED          -> "Incident has been classified and categorised";
            case WAITING_FOR_DISPATCH-> "Incident is queued and awaiting unit dispatch";
            case DISPATCHED          -> "Response units have been dispatched";
            case ACKNOWLEDGED        -> "Responding units have acknowledged the dispatch";
            case EN_ROUTE            -> "Responding units are en route to the scene";
            case AT_SCENE            -> "Responding units have arrived at the scene";
            case IN_PROGRESS         -> "Incident response is in progress";
            case RESOLVED            -> "Incident has been resolved";
            case CLOSED              -> "Incident has been officially closed";
            case REJECTED            -> "Incident report was rejected";
            default                  -> fromLabel + " → " + toLabel;
        };
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
