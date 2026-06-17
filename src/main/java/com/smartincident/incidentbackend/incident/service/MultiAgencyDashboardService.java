package com.smartincident.incidentbackend.incident.service;

import com.smartincident.incidentbackend.enums.AgencyResponseStatus;
import com.smartincident.incidentbackend.enums.IncidentStatus;
import com.smartincident.incidentbackend.incident.dto.MultiAgencyDashboardDto;
import com.smartincident.incidentbackend.incident.entity.IncidentAgency;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.incident.repository.IncidentAgencyRepository;
import com.smartincident.incidentbackend.incident.repository.IncidentReportRepository;
import com.smartincident.incidentbackend.setting.entity.Agency;
import com.smartincident.incidentbackend.setting.repository.AgencyRepository;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponseList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MultiAgencyDashboardService {

    private final IncidentReportRepository incidentRepository;
    private final IncidentAgencyRepository incidentAgencyRepository;
    private final AgencyRepository agencyRepository;

    private static final Set<IncidentStatus> ACTIVE_STATUSES = EnumSet.of(
            IncidentStatus.REPORTED, IncidentStatus.PENDING, IncidentStatus.UNDER_REVIEW,
            IncidentStatus.CLASSIFIED, IncidentStatus.WAITING_FOR_DISPATCH,
            IncidentStatus.DISPATCHED, IncidentStatus.ACKNOWLEDGED,
            IncidentStatus.EN_ROUTE, IncidentStatus.AT_SCENE, IncidentStatus.IN_PROGRESS
    );

    public ResponseList<MultiAgencyDashboardDto> getActiveMultiAgencyIncidents() {
        List<IncidentReport> incidents = incidentRepository.findAll().stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsActive())
                        && ACTIVE_STATUSES.contains(i.getStatus())
                        && i.getInvolvedAgencies() != null
                        && !i.getInvolvedAgencies().isEmpty())
                .sorted(Comparator.comparing((IncidentReport i) -> i.getEmergencyLevel().ordinal()).reversed()
                        .thenComparing(IncidentReport::getReportedAt, Comparator.reverseOrder()))
                .toList();

        List<MultiAgencyDashboardDto> dtos = incidents.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return new ResponseList<>(dtos);
    }

    public Response<MultiAgencyDashboardDto> getIncidentCoordination(String incidentUid) {
        return incidentRepository.findByUid(incidentUid)
                .map(i -> Response.success(toDto(i)))
                .orElseGet(() -> Response.error("Incident not found"));
    }

    public Response<MultiAgencyDashboardDto.DashboardSummary> getDashboardSummary() {
        List<IncidentReport> allActive = incidentRepository.findAll().stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsActive()) && ACTIVE_STATUSES.contains(i.getStatus()))
                .toList();

        long multiAgencyCount = allActive.stream()
                .filter(i -> i.getInvolvedAgencies() != null && i.getInvolvedAgencies().size() > 1)
                .count();

        List<Agency> agencies = agencyRepository.findAll();
        List<MultiAgencyDashboardDto.AgencyResponseSummary> agencyStats = agencies.stream()
                .map(agency -> {
                    List<IncidentAgency> records = incidentAgencyRepository
                            .findByAgencyAndResponseStatusIn(agency, List.of(
                                    AgencyResponseStatus.NOTIFIED,
                                    AgencyResponseStatus.RESPONDING,
                                    AgencyResponseStatus.ON_SITE,
                                    AgencyResponseStatus.COMPLETED,
                                    AgencyResponseStatus.REJECTED));
                    return MultiAgencyDashboardDto.AgencyResponseSummary.builder()
                            .agencyCode(agency.getCode())
                            .agencyName(agency.getName())
                            .notified(records.stream().filter(r -> r.getResponseStatus() == AgencyResponseStatus.NOTIFIED).count())
                            .responding(records.stream().filter(r -> r.getResponseStatus() == AgencyResponseStatus.RESPONDING
                                    || r.getResponseStatus() == AgencyResponseStatus.ON_SITE).count())
                            .completed(records.stream().filter(r -> r.getResponseStatus() == AgencyResponseStatus.COMPLETED).count())
                            .unavailable(records.stream().filter(r -> r.getResponseStatus() == AgencyResponseStatus.REJECTED).count())
                            .build();
                })
                .collect(Collectors.toList());

        MultiAgencyDashboardDto.DashboardSummary summary = MultiAgencyDashboardDto.DashboardSummary.builder()
                .totalActive(allActive.size())
                .majorIncidents(allActive.stream().filter(i -> Boolean.TRUE.equals(i.getIsMajorIncident())).count())
                .eocActivated(allActive.stream().filter(i -> Boolean.TRUE.equals(i.getEocActivated())).count())
                .slaBreached(allActive.stream().filter(i -> i.getSlaBreachedAt() != null).count())
                .multiAgencyEngaged(multiAgencyCount)
                .agencyResponses(agencyStats)
                .build();

        return Response.success(summary);
    }

    private MultiAgencyDashboardDto toDto(IncidentReport incident) {
        List<MultiAgencyDashboardDto.AgencyCoordinationEntry> entries = Optional
                .ofNullable(incident.getInvolvedAgencies()).orElse(List.of())
                .stream()
                .map(ia -> MultiAgencyDashboardDto.AgencyCoordinationEntry.builder()
                        .agencyUid(ia.getAgency() != null ? ia.getAgency().getUid() : null)
                        .agencyName(ia.getAgency() != null ? ia.getAgency().getName() : null)
                        .agencyCode(ia.getAgency() != null ? ia.getAgency().getCode() : null)
                        .role(ia.getAgencyRole())
                        .responseStatus(ia.getResponseStatus())
                        .responsibleOfficerName(ia.getResponsibleOfficer() != null
                                ? ia.getResponsibleOfficer().getName() : null)
                        .notifiedAt(ia.getNotifiedAt())
                        .respondedAt(ia.getRespondedAt())
                        .arrivedAt(ia.getArrivedAt())
                        .completedAt(ia.getCompletedAt())
                        .statusNotes(ia.getStatusNotes())
                        .build())
                .collect(Collectors.toList());

        return MultiAgencyDashboardDto.builder()
                .incidentUid(incident.getUid())
                .incidentTitle(incident.getTitle())
                .incidentStatus(incident.getStatus())
                .emergencyLevel(incident.getEmergencyLevel())
                .isMajorIncident(Boolean.TRUE.equals(incident.getIsMajorIncident()))
                .eocActivated(Boolean.TRUE.equals(incident.getEocActivated()))
                .location(incident.getLocation())
                .reportedAt(incident.getReportedAt())
                .slaDeadline(incident.getSlaDeadline())
                .slaBreachedAt(incident.getSlaBreachedAt())
                .incidentCommanderName(incident.getIncidentCommander() != null
                        ? incident.getIncidentCommander().getName() : null)
                .agencies(entries)
                .build();
    }
}
