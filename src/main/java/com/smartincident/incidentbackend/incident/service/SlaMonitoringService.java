package com.smartincident.incidentbackend.incident.service;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.enums.IncidentStatus;
import com.smartincident.incidentbackend.enums.NotificationChannel;
import com.smartincident.incidentbackend.enums.NotificationType;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.incident.repository.IncidentReportRepository;
import com.smartincident.incidentbackend.notification.dto.NotificationDto;
import com.smartincident.incidentbackend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlaMonitoringService {

    private final IncidentReportRepository incidentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /** Active statuses that have not yet been resolved — SLA clock still running. */
    private static final Set<IncidentStatus> ACTIVE_STATUSES = EnumSet.of(
            IncidentStatus.REPORTED,
            IncidentStatus.PENDING,
            IncidentStatus.UNDER_REVIEW,
            IncidentStatus.CLASSIFIED,
            IncidentStatus.WAITING_FOR_DISPATCH,
            IncidentStatus.DISPATCHED,
            IncidentStatus.ACKNOWLEDGED,
            IncidentStatus.EN_ROUTE,
            IncidentStatus.AT_SCENE,
            IncidentStatus.IN_PROGRESS
    );

    /** Run every 60 seconds — check for SLA breaches and escalate. */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void checkSlaBreaches() {
        LocalDateTime now = LocalDateTime.now();

        List<IncidentReport> candidates = incidentRepository.findSlaBreachCandidates(ACTIVE_STATUSES, now);

        if (candidates.isEmpty()) return;

        log.warn("SLA breach detected for {} incident(s)", candidates.size());

        List<String> supervisorUids = userRepository.findByRoleInAndIsActiveTrue(
                        List.of(Role.DISPATCHER_SUPERVISOR, Role.DISPATCH_CENTER_ADMIN, Role.AGENCY_ADMIN))
                .stream().map(User::getUid).toList();

        for (IncidentReport incident : candidates) {
            incident.setSlaBreachedAt(now);
            incidentRepository.save(incident);

            long minutesOverdue = java.time.Duration.between(incident.getSlaDeadline(), now).toMinutes();
            log.warn("SLA breached: incident={} level={} overdue={}min",
                    incident.getUid(), incident.getEmergencyLevel(), minutesOverdue);

            if (!supervisorUids.isEmpty()) {
                NotificationDto dto = new NotificationDto();
                dto.setTitle("SLA BREACH — " + incident.getEmergencyLevel().name() + " Incident");
                dto.setMessage("Incident \"" + incident.getTitle() + "\" is " + minutesOverdue
                        + " minute(s) overdue. Current status: " + incident.getStatus()
                        + ". Immediate supervisor review required.");
                dto.setType(NotificationType.INCIDENT_ASSIGNED);
                dto.setChannels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH));
                dto.setRelatedEntityUid(incident.getUid());
                dto.setRelatedEntityType("INCIDENT");
                dto.setTargetUserUids(supervisorUids);
                notificationService.sendNotification(dto);
            }
        }
    }

    /** Run every 5 minutes — warn about incidents approaching SLA deadline (within 2 min). */
    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void warnApproachingSla() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime warningThreshold = now.plusMinutes(2);

        List<IncidentReport> approaching = incidentRepository.findSlaApproachingCandidates(ACTIVE_STATUSES, now, warningThreshold);

        if (approaching.isEmpty()) return;

        List<String> dispatcherUids = userRepository.findByRoleInAndIsActiveTrue(
                        List.of(Role.DISPATCHER, Role.DISPATCHER_SUPERVISOR, Role.DISPATCH_CENTER_ADMIN))
                .stream().map(User::getUid).toList();

        for (IncidentReport incident : approaching) {
            long secsLeft = java.time.Duration.between(now, incident.getSlaDeadline()).toSeconds();
            if (dispatcherUids.isEmpty()) continue;
            NotificationDto dto = new NotificationDto();
            dto.setTitle("SLA Warning — " + incident.getEmergencyLevel().name());
            dto.setMessage("Incident \"" + incident.getTitle() + "\" SLA deadline in ~" + secsLeft + "s. Status: "
                    + incident.getStatus());
            dto.setType(NotificationType.INCIDENT_ASSIGNED);
            dto.setChannels(List.of(NotificationChannel.IN_APP));
            dto.setRelatedEntityUid(incident.getUid());
            dto.setRelatedEntityType("INCIDENT");
            dto.setTargetUserUids(dispatcherUids);
            notificationService.sendNotification(dto);
        }
    }
}
