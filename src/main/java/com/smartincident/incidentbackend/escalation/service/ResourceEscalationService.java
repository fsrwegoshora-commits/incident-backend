package com.smartincident.incidentbackend.escalation.service;

import com.smartincident.incidentbackend.enums.*;
import com.smartincident.incidentbackend.escalation.dto.ResourceEscalationDto;
import com.smartincident.incidentbackend.escalation.entity.ResourceEscalation;
import com.smartincident.incidentbackend.escalation.repository.ResourceEscalationRepository;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.incident.repository.IncidentReportRepository;
import com.smartincident.incidentbackend.notification.dto.NotificationDto;
import com.smartincident.incidentbackend.notification.service.NotificationService;
import com.smartincident.incidentbackend.utils.LoggedUser;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponseList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceEscalationService {

    private final ResourceEscalationRepository repository;
    private final IncidentReportRepository incidentRepository;
    private final NotificationService notificationService;

    public Response<ResourceEscalation> escalate(ResourceEscalationDto dto) {
        Optional<IncidentReport> incidentOpt = incidentRepository.findByUid(dto.getIncidentUid());
        if (incidentOpt.isEmpty()) return Response.error("Incident not found");

        ResourceEscalation escalation = ResourceEscalation.builder()
                .incident(incidentOpt.get())
                .level(dto.getLevel())
                .resourceType(dto.getResourceType())
                .reason(dto.getReason())
                .requestedByUid(LoggedUser.getUid())
                .requestedByName(LoggedUser.getName())
                .requestedByRole(LoggedUser.getRole() != null ? LoggedUser.getRole().name() : null)
                .status(ResourceEscalationStatus.PENDING)
                .build();

        ResourceEscalation saved = repository.save(escalation);
        notifyRecipients(saved);
        return Response.success(saved);
    }

    public Response<ResourceEscalation> respond(String uid, String responseNotes, ResourceEscalationStatus status) {
        Optional<ResourceEscalation> opt = repository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Escalation not found");

        ResourceEscalation esc = opt.get();
        esc.setStatus(status);
        esc.setResponseNotes(responseNotes);
        esc.setRespondedByUid(LoggedUser.getUid());
        esc.setRespondedByName(LoggedUser.getName());
        esc.setRespondedAt(LocalDateTime.now());
        return Response.success(repository.save(esc));
    }

    public ResponseList<ResourceEscalation> getByIncident(String incidentUid) {
        return new ResponseList<>(repository.findByIncidentUidOrderByCreatedAtDesc(incidentUid));
    }

    public ResponseList<ResourceEscalation> getPending() {
        return new ResponseList<>(repository.findByStatusAndIsActiveTrue(ResourceEscalationStatus.PENDING));
    }

    private void notifyRecipients(ResourceEscalation esc) {
        Role targetRole = switch (esc.getLevel()) {
            case POST_TO_STATION    -> Role.STATION_ADMIN;
            case STATION_TO_AGENCY  -> Role.AGENCY_ADMIN;
            case AGENCY_TO_NATIONAL -> Role.ROOT;
        };

        NotificationDto dto = new NotificationDto();
        dto.setTitle("Resource Escalation — " + levelLabel(esc.getLevel()));
        dto.setMessage("Incident: " + esc.getIncident().getTitle()
                + " | Resource needed: " + esc.getResourceType()
                + " | Reason: " + esc.getReason());
        dto.setType(NotificationType.INCIDENT_UPDATE);
        dto.setChannels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH));
        dto.setTargetRole(targetRole);
        dto.setRelatedEntityUid(esc.getIncident().getUid());
        dto.setRelatedEntityType("INCIDENT");

        try {
            notificationService.sendNotificationByRoleAndStation(dto);
        } catch (Exception e) {
            log.warn("Failed to send resource escalation notification: {}", e.getMessage());
        }
    }

    private String levelLabel(EscalationLevel level) {
        return switch (level) {
            case POST_TO_STATION    -> "Post → Station";
            case STATION_TO_AGENCY  -> "Station → Agency";
            case AGENCY_TO_NATIONAL -> "Agency → National";
        };
    }
}
