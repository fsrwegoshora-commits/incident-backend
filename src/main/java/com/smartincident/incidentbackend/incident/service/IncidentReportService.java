package com.smartincident.incidentbackend.incident.service;

import com.smartincident.incidentbackend.incident.dto.IncidentReportDto;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.incident.repository.IncidentReportRepository;
import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
import com.smartincident.incidentbackend.police.entity.PoliceOfficer;
import com.smartincident.incidentbackend.police.repository.PoliceStationRepository;
import com.smartincident.incidentbackend.police.repository.PoliceOfficerRepository;
import com.smartincident.incidentbackend.enums.IncidentStatus;
import com.smartincident.incidentbackend.utils.*;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@GraphQLApi
public class IncidentReportService {

    private final IncidentReportRepository incidentRepository;
    private final UserRepository userRepository;
    private final PoliceStationRepository stationRepository;
    private final PoliceOfficerRepository officerRepository;
    // private final NotificationService notificationService;  // For push notifications

    @Transactional
    public Response<IncidentReport> createIncident(IncidentReportDto dto) {
        log.info("Creating incident report: {}", dto.getTitle());

        // Validation
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            return Response.error("Title is required");
        }
        if (dto.getType() == null) {
            return Response.error("Incident type is required");
        }
        if (dto.getLocation() == null || dto.getLocation().trim().isEmpty()) {
            return Response.error("Location is required");
        }
        if (dto.getLatitude() == null || dto.getLongitude() == null) {
            return Response.error("GPS coordinates are required");
        }
        if (dto.getAssignedStationUid() == null) {
            return Response.error("Police station must be selected");
        }

        // Get reporter (logged in user)
        String loggedUserUid = LoggedUser.getUid();
        if (loggedUserUid == null) {
            return Response.error("User not authenticated");
        }

        Optional<User> reporterOpt = userRepository.findByUid(loggedUserUid);
        if (!reporterOpt.isPresent()) {
            return Response.error("Reporter not found");
        }
        User reporter = reporterOpt.get();

        PoliceOfficer officer =null;
        if (dto.getAssignedOfficerUid() != null && !dto.getAssignedOfficerUid().isEmpty()) {
            Optional<PoliceOfficer> officerOpt = officerRepository.findByUid(dto.getAssignedOfficerUid());
            if (!officerOpt.isPresent()) {
                return Response.error("Assigned officer not found");
            }
            officer = officerOpt.get();

            if (!officer.getStation().getUid().equals(dto.getAssignedStationUid()))
                return Response.error("Officer does not belong to the selected station");

            log.info("Assigning incident to officer: {}", officer.getBadgeNumber());
        }

        // Get assigned station
        Optional<PoliceStation> stationOpt = stationRepository.findByUid(dto.getAssignedStationUid());
        if (!stationOpt.isPresent()) {
            return Response.error("Police station not found");
        }
        PoliceStation station = stationOpt.get();

        // Create incident
        IncidentReport incident = new IncidentReport();
        incident.setTitle(dto.getTitle());
        incident.setDescription(dto.getDescription());
        incident.setType(dto.getType());
        incident.setLocation(dto.getLocation());
        incident.setLatitude(dto.getLatitude());
        incident.setLongitude(dto.getLongitude());
        incident.setImageUrl(dto.getImageUrl());
        incident.setAudioUrl(dto.getAudioUrl());
        incident.setVideoUrl(dto.getVideoUrl());
        incident.setLiveCallRequested(dto.isLiveCallRequested());
        incident.setStatus(IncidentStatus.PENDING);
        incident.setReportedBy(reporter);
        incident.setAssignedStation(station);
        if (officer != null) {
            incident.setAssignedOfficer(officer);
        }
        incident.setReportedAt(LocalDateTime.now());

        try {
            incident = incidentRepository.save(incident);
            log.info("✅ Incident created successfully: {}", incident.getUid());
            // notificationService.notifyStationAdmins(station.getUid(), incident);

            return new Response<>(incident);
        } catch (Exception e) {
            log.error("Failed to create incident: {}", e.getMessage());
            return Response.error("Failed to report incident: " + Utils.getExceptionMessage(e));
        }
    }


    @Transactional
    public Response<IncidentReport> updateIncident(IncidentReportDto dto) {
        log.info("Updating incident: {}", dto.getUid());

        if (dto.getUid() == null) {
            return Response.error("Incident UID is required");
        }

        Optional<IncidentReport> incidentOpt = incidentRepository.findByUid(dto.getUid());
        if (!incidentOpt.isPresent()) {
            return Response.error("Incident not found");
        }

        IncidentReport incident = incidentOpt.get();

        // Update fields
        if (dto.getTitle() != null) {
            incident.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            incident.setDescription(dto.getDescription());
        }
        if (dto.getStatus() != null) {
            incident.setStatus(dto.getStatus());

            // If resolved, set resolved time
            if (dto.getStatus() == IncidentStatus.RESOLVED) {
                incident.setResolvedAt(LocalDateTime.now());
            }
        }
        if (dto.getAssignedOfficerUid() != null) {
            Optional<PoliceOfficer> officerOpt = officerRepository.findByUid(dto.getAssignedOfficerUid());
            if (officerOpt.isPresent()) {
                incident.setAssignedOfficer(officerOpt.get());
            }
        }

        incident.update();

        try {
            incident = incidentRepository.save(incident);
            log.info(" Incident updated successfully");

            // notificationService.notifyReporter(incident);

            return new Response<>(incident);
        } catch (Exception e) {
            log.error("Failed to update incident: {}", e.getMessage());
            return Response.error("Failed to update incident: " + Utils.getExceptionMessage(e));
        }
    }

    @Transactional
    public Response<IncidentReport> assignOfficer(String incidentUid, String officerUid) {
        log.info("Assigning officer {} to incident {}", officerUid, incidentUid);

        Optional<IncidentReport> incidentOpt = incidentRepository.findByUid(incidentUid);
        if (!incidentOpt.isPresent()) {
            return Response.error("Incident not found");
        }

        Optional<PoliceOfficer> officerOpt = officerRepository.findByUid(officerUid);
        if (!officerOpt.isPresent()) {
            return Response.error("Officer not found");
        }

        IncidentReport incident = incidentOpt.get();
        PoliceOfficer officer = officerOpt.get();

        incident.setAssignedOfficer(officer);
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incident.update();

        try {
            incident = incidentRepository.save(incident);
            log.info("✅ Officer assigned successfully");

            // TODO: Notify officer & reporter
            // notificationService.notifyOfficerAssignment(officer, incident);

            return new Response<>(incident+"Officer assigned successfully");
        } catch (Exception e) {
            log.error(" Failed to assign officer: {}", e.getMessage());
            return Response.error("Failed to assign officer: " + Utils.getExceptionMessage(e));
        }
    }

    public Response<IncidentReport> getIncident(String uid) {
        if (uid == null) {
            return Response.error("Incident UID is required");
        }

        Optional<IncidentReport> incident = incidentRepository.findByUid(uid);
        if (incident.isPresent()) {
            return Response.success(incident.get());
        }

        return Response.error("Incident not found");
    }

    public ResponsePage<IncidentReport> getMyIncidents(PageableParam pageableParam) {
        String userUid = LoggedUser.getUid();
        if (userUid == null) {
            return new ResponsePage<>("User not authenticated");
        }

        Page<IncidentReport> incidents = incidentRepository.findByReporter(
                userUid,
                pageableParam.getIsActive(),
                pageableParam.key(),
                pageableParam.getPageable(true)
        );

        return new ResponsePage<>(incidents);
    }

    public ResponsePage<IncidentReport> getStationIncidents(PageableParam pageableParam, IncidentStatus status) {
        String stationUid = LoggedUser.getStationUid();
        if (stationUid == null) {
            return new ResponsePage<>("Station not found");
        }

        Page<IncidentReport> incidents = incidentRepository.findByStation(
                stationUid,
                status,
                pageableParam.getIsActive(),
                pageableParam.key(),
                pageableParam.getPageable(true)
        );

        return new ResponsePage<>(incidents);
    }


    public ResponsePage<IncidentReport> getOfficerIncidents(PageableParam pageableParam, IncidentStatus status) {
        String officerUid = LoggedUser.getOfficerUid();
        if (officerUid == null) {
            return new ResponsePage<>("Officer not found");
        }
        Page<IncidentReport> incidents = incidentRepository.findByOfficer(
                officerUid,
                status,
                pageableParam.getIsActive(),
                pageableParam.key(),
                pageableParam.getPageable(true)
        );

        return new ResponsePage<>(incidents);
    }

    public Response<List<IncidentReport>> getNearbyIncidents(
            Double latitude,
            Double longitude,
            Double radiusKm,
            IncidentStatus status
    ) {
        if (latitude == null || longitude == null) {
            return Response.error("Coordinates are required");
        }

        if (radiusKm == null || radiusKm <= 0) {
            radiusKm = 10.0;  // Default 10km
        }

        List<IncidentReport> incidents = incidentRepository.findNearbyIncidents(
                latitude,
                longitude,
                radiusKm,
                status != null ? status.name() : null
        );

        return Response.success(incidents);
    }

    @Transactional
    public Response<IncidentReport> deleteIncident(String uid) {
        if (uid == null) {
            return Response.error("Incident UID is required");
        }

        Optional<IncidentReport> incidentOpt = incidentRepository.findByUid(uid);
        if (!incidentOpt.isPresent()) {
            return Response.error("Incident not found");
        }

        IncidentReport incident = incidentOpt.get();

        if (!incident.getIsActive()) {
            return Response.error("Incident already deleted");
        }

        incident.delete();

        try {
            incidentRepository.save(incident);
            log.info("Incident deleted successfully");
            return new Response<>(incident+ "Incident deleted successfully");
        } catch (Exception e) {
            log.error(" Failed to delete incident: {}", e.getMessage());
            return Response.error("Failed to delete incident: " + Utils.getExceptionMessage(e));
        }
    }

    public Response<IncidentStats> getStationStats(String stationUid) {
        if (stationUid == null) {
            stationUid = LoggedUser.getStationUid();
        }

        if (stationUid == null) {
            return Response.error("Station not found");
        }

        IncidentStats stats = new IncidentStats();
        stats.setPending(incidentRepository.countByStationAndStatus(stationUid, IncidentStatus.PENDING));
        stats.setInProgress(incidentRepository.countByStationAndStatus(stationUid, IncidentStatus.IN_PROGRESS));
        stats.setResolved(incidentRepository.countByStationAndStatus(stationUid, IncidentStatus.RESOLVED));

        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        List<IncidentReport> recentIncidents = incidentRepository.findRecentIncidents(last24Hours, stationUid);
        stats.setRecentCount((long) recentIncidents.size());

        return Response.success(stats);
    }

    // Inner class for statistics
    @lombok.Data
    public static class IncidentStats {
        private Long pending;
        private Long inProgress;
        private Long resolved;
        private Long recentCount;
    }
}