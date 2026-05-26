package com.smartincident.incidentbackend.incident.controller;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.enums.*;
import com.smartincident.incidentbackend.incident.dto.IncidentAgencyDto;
import com.smartincident.incidentbackend.incident.entity.IncidentAgency;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.incident.repository.IncidentAgencyRepository;
import com.smartincident.incidentbackend.incident.repository.IncidentReportRepository;
import com.smartincident.incidentbackend.notification.dto.NotificationDto;
import com.smartincident.incidentbackend.notification.service.NotificationService;
import com.smartincident.incidentbackend.setting.entity.Agency;
import com.smartincident.incidentbackend.setting.repository.AgencyRepository;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponseList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
public class IncidentAgencyController {

    private final IncidentReportRepository incidentRepository;
    private final IncidentAgencyRepository incidentAgencyRepository;
    private final AgencyRepository agencyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Dispatcher dispatches an agency to an incident.
     * Creates / updates IncidentAgency and moves the incident to DISPATCHED status.
     *
     * Body: { incidentUid, agencyUid, agencyType, notes? }
     */
    @Authenticated
    @AuthorizedRole({Role.DISPATCHER, Role.ROOT, Role.AGENCY_ADMIN, Role.STATION_ADMIN})
    @PostMapping("/api/incidents/dispatch")
    public Response<IncidentAgency> dispatchAgency(@RequestBody Map<String, String> body) {
        String incidentUid = body.get("incidentUid");
        String agencyUid   = body.get("agencyUid");
        String agencyType  = body.get("agencyType");
        String notes       = body.get("notes");

        if (incidentUid == null) return Response.error("incidentUid is required");
        if (agencyUid   == null && agencyType == null)
            return Response.error("agencyUid or agencyType is required");

        Optional<IncidentReport> incidentOpt = incidentRepository.findByUid(incidentUid);
        if (incidentOpt.isEmpty()) return Response.error("Incident not found");
        IncidentReport incident = incidentOpt.get();

        // Resolve agency
        Optional<Agency> agencyOpt;
        if (agencyUid != null) {
            agencyOpt = agencyRepository.findByUid(agencyUid);
        } else {
            agencyOpt = agencyRepository.findByCode(agencyType);
        }
        if (agencyOpt.isEmpty()) return Response.error("Agency not found");
        Agency agency = (Agency) agencyOpt.get();

        // Find existing IncidentAgency or create one
        List<IncidentAgency> existing = incidentAgencyRepository.findByIncidentAndAgency(incident, agency);
        IncidentAgency ia = existing.isEmpty() ? new IncidentAgency() : existing.get(0);

        if (existing.isEmpty()) {
            ia.setIncident(incident);
            ia.setAgency(agency);
            ia.setAgencyRole(AgencyRole.SUPPORT);
            ia.setNotifiedAt(LocalDateTime.now());
        }

        ia.setResponseStatus(AgencyResponseStatus.RESPONDING);
        ia.setRespondedAt(LocalDateTime.now());
        if (notes != null && !notes.isBlank()) ia.setStatusNotes(notes);
        ia.update();

        try {
            ia = incidentAgencyRepository.save(ia);
        } catch (Exception e) {
            log.error("Failed to save IncidentAgency: {}", e.getMessage());
            return Response.error("Failed to dispatch agency: " + e.getMessage());
        }

        // Move incident to DISPATCHED if it hasn't progressed further yet
        if (incident.getStatus() == IncidentStatus.PENDING
                || incident.getStatus() == IncidentStatus.REPORTED
                || incident.getStatus() == IncidentStatus.WAITING_FOR_DISPATCH) {
            incident.setStatus(IncidentStatus.DISPATCHED);
            incident.update();
            incidentRepository.save(incident);
        }

        // Notify agency staff
        notifyAgencyStaff(incident, agency, agencyType);

        log.info("Agency {} dispatched to incident {}", agency.getName(), incident.getUid());
        return Response.success(ia);
    }

    /** List all agency involvements for a specific incident. */
    @Authenticated
    @GetMapping("/api/incidents/{incidentUid}/agencies")
    public ResponseList<IncidentAgency> getIncidentAgencies(@PathVariable String incidentUid) {
        Optional<IncidentReport> incidentOpt = incidentRepository.findByUid(incidentUid);
        if (incidentOpt.isEmpty()) return ResponseList.error("Incident not found");
        return new ResponseList<>(incidentAgencyRepository.findByIncident(incidentOpt.get()));
    }

    /** Update the response status of an IncidentAgency record. */
    @Authenticated
    @AuthorizedRole({Role.DISPATCHER, Role.ROOT, Role.AGENCY_ADMIN, Role.STATION_ADMIN})
    @PutMapping("/api/incident-agencies/{uid}/status")
    public Response<IncidentAgency> updateStatus(
            @PathVariable String uid,
            @RequestBody IncidentAgencyDto dto) {

        Optional<IncidentAgency> opt = incidentAgencyRepository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("IncidentAgency record not found");

        IncidentAgency ia = opt.get();
        if (dto.getResponseStatus() != null) ia.setResponseStatus(dto.getResponseStatus());
        if (dto.getStatusNotes()    != null) ia.setStatusNotes(dto.getStatusNotes());
        if (dto.getArrivedAt()      != null) ia.setArrivedAt(dto.getArrivedAt());
        if (dto.getCompletedAt()    != null) ia.setCompletedAt(dto.getCompletedAt());
        ia.update();

        try {
            ia = incidentAgencyRepository.save(ia);
            return Response.success(ia);
        } catch (Exception e) {
            return Response.error("Failed to update status: " + e.getMessage());
        }
    }

    private void notifyAgencyStaff(IncidentReport incident, Agency agency, String agencyType) {
        if (agencyType == null) return;
        Role targetRole = switch (agencyType) {
            case "POLICE"  -> Role.POLICE_OFFICER;
            case "FIRE"    -> Role.FIRE_OFFICER;
            case "MEDICAL" -> Role.MEDIC;
            default        -> null;
        };
        if (targetRole == null) return;

        List<String> userUids = userRepository.findByRoleAndIsActiveTrue(targetRole)
                .stream().map(User::getUid).toList();
        if (userUids.isEmpty()) return;

        NotificationDto dto = new NotificationDto();
        dto.setTitle("Agency Dispatched — " + agencyType);
        dto.setMessage("Your agency has been dispatched to: " + incident.getTitle()
                + " at " + incident.getLocation());
        dto.setType(NotificationType.INCIDENT_ASSIGNED);
        dto.setChannels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH));
        dto.setRelatedEntityUid(incident.getUid());
        dto.setRelatedEntityType("INCIDENT");
        dto.setTargetUserUids(userUids);
        notificationService.sendNotification(dto);
    }
}
