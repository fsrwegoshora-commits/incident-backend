package com.smartincident.incidentbackend.operations.service;

import com.smartincident.incidentbackend.dispatcher.entity.DispatcherShift;
import com.smartincident.incidentbackend.dispatcher.repository.DispatcherShiftRepository;
import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.emergency.repository.EmergencyUnitRepository;
import com.smartincident.incidentbackend.emergency.repository.EmergencyVehicleRepository;
import com.smartincident.incidentbackend.enums.*;
import com.smartincident.incidentbackend.fire.entity.FireOfficer;
import com.smartincident.incidentbackend.fire.repository.FireOfficerRepository;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.incident.repository.IncidentReportRepository;
import com.smartincident.incidentbackend.medical.entity.Medic;
import com.smartincident.incidentbackend.medical.repository.MedicRepository;
import com.smartincident.incidentbackend.operations.dto.OperationsDashboardDto;
import com.smartincident.incidentbackend.police.entity.OfficerShift;
import com.smartincident.incidentbackend.police.repository.OfficerShiftRepository;
import com.smartincident.incidentbackend.utils.LoggedUser;
import com.smartincident.incidentbackend.utils.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperationsDashboardService {

    private final IncidentReportRepository incidentRepository;
    private final DispatcherShiftRepository dispatcherShiftRepository;
    private final OfficerShiftRepository officerShiftRepository;
    private final FireOfficerRepository fireOfficerRepository;
    private final MedicRepository medicRepository;
    private final EmergencyUnitRepository emergencyUnitRepository;
    private final EmergencyVehicleRepository vehicleRepository;

    private static final List<IncidentStatus> ACTIVE_STATUSES = List.of(
            IncidentStatus.REPORTED, IncidentStatus.UNDER_REVIEW, IncidentStatus.CLASSIFIED,
            IncidentStatus.WAITING_FOR_DISPATCH, IncidentStatus.DISPATCHED,
            IncidentStatus.ACKNOWLEDGED, IncidentStatus.EN_ROUTE,
            IncidentStatus.AT_SCENE, IncidentStatus.IN_PROGRESS);

    public Response<OperationsDashboardDto> getDashboard() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        LocalDateTime nowDt = LocalDateTime.now();

        List<IncidentReport> allIncidents = incidentRepository.findAll();
        List<IncidentReport> active = allIncidents.stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsActive()) && ACTIVE_STATUSES.contains(i.getStatus()))
                .toList();

        // Scope by agency if AGENCY_ADMIN
        Role callerRole = LoggedUser.getRole();
        String agencyUid = LoggedUser.getAgencyUid();
        if (callerRole == Role.AGENCY_ADMIN && agencyUid != null) {
            active = active.stream()
                    .filter(i -> i.getLeadAgency() != null && agencyUid.equals(i.getLeadAgency().getUid()))
                    .toList();
        }

        final List<IncidentReport> scopedActive = active;

        // On-duty counts
        int onDutyDispatchers = dispatcherShiftRepository.findOnDutyNow(today, now).size();
        int onDutyOfficers = (int) officerShiftRepository.findByDate(today)
                .stream().filter(s -> isOnDuty(s.getStartTime(), s.getEndTime(), now)).count();
        int onDutyFire = (int) fireOfficerRepository.findAll().stream()
                .filter(f -> Boolean.TRUE.equals(f.getIsOnDuty()) && Boolean.TRUE.equals(f.getIsActive())).count();
        int onDutyMedics = (int) medicRepository.findAll().stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsOnDuty()) && Boolean.TRUE.equals(m.getIsActive())).count();

        long slaBreached = scopedActive.stream().filter(i -> i.getSlaBreachedAt() != null).count();
        long slaApproaching = scopedActive.stream()
                .filter(i -> i.getSlaDeadline() != null
                        && i.getSlaDeadline().isAfter(nowDt)
                        && i.getSlaDeadline().isBefore(nowDt.plusMinutes(15))
                        && i.getSlaBreachedAt() == null)
                .count();

        OperationsDashboardDto.DashboardSummary summary = OperationsDashboardDto.DashboardSummary.builder()
                .totalActive((long) scopedActive.size())
                .critical(count(scopedActive, EmergencyLevel.CRITICAL))
                .high(count(scopedActive, EmergencyLevel.HIGH))
                .medium(count(scopedActive, EmergencyLevel.MEDIUM))
                .low(count(scopedActive, EmergencyLevel.LOW))
                .slaBreached(slaBreached)
                .slaApproaching(slaApproaching)
                .dispatched(countStatus(scopedActive, IncidentStatus.DISPATCHED))
                .enRoute(countStatus(scopedActive, IncidentStatus.EN_ROUTE))
                .atScene(countStatus(scopedActive, IncidentStatus.AT_SCENE))
                .inProgress(countStatus(scopedActive, IncidentStatus.IN_PROGRESS))
                .onDutyDispatchers(onDutyDispatchers)
                .onDutyOfficers(onDutyOfficers)
                .onDutyFireOfficers(onDutyFire)
                .onDutyMedics(onDutyMedics)
                .build();

        List<OperationsDashboardDto.ActiveIncidentSummary> incidentSummaries = scopedActive.stream()
                .map(i -> OperationsDashboardDto.ActiveIncidentSummary.builder()
                        .uid(i.getUid())
                        .title(i.getTitle())
                        .type(i.getType() != null ? i.getType().name() : null)
                        .status(i.getStatus().name())
                        .emergencyLevel(i.getEmergencyLevel() != null ? i.getEmergencyLevel().name() : null)
                        .location(i.getLocation())
                        .latitude(i.getLatitude())
                        .longitude(i.getLongitude())
                        .reportedAt(i.getReportedAt())
                        .slaDeadline(i.getSlaDeadline())
                        .slaBreached(i.getSlaBreachedAt() != null)
                        .assignedDispatcher(i.getAssignedDispatcher() != null ? i.getAssignedDispatcher().getName() : null)
                        .assignedUnit(i.getAssignedUnit() != null ? i.getAssignedUnit().getName() : null)
                        .assignedPoliceStation(i.getAssignedPoliceStation() != null ? i.getAssignedPoliceStation().getName() : null)
                        .isMajorIncident(i.getIsMajorIncident())
                        .eocActivated(i.getEocActivated())
                        .incidentCommander(i.getIncidentCommander() != null ? i.getIncidentCommander().getName() : null)
                        .build())
                .toList();

        // Unit status view
        List<OperationsDashboardDto.UnitStatus> unitStatuses = emergencyUnitRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .map(u -> buildUnitStatus(u, scopedActive))
                .toList();

        return Response.success(OperationsDashboardDto.builder()
                .summary(summary)
                .activeIncidents(incidentSummaries)
                .units(unitStatuses)
                .build());
    }

    private OperationsDashboardDto.UnitStatus buildUnitStatus(EmergencyUnit unit, List<IncidentReport> active) {
        int activeForUnit = (int) active.stream()
                .filter(i -> i.getAssignedUnit() != null && unit.getUid().equals(i.getAssignedUnit().getUid()))
                .count();
        int availableVehicles = vehicleRepository.findByUnitUidAndStatus(unit.getUid(), VehicleStatus.AVAILABLE).size();
        int onDutyPersonnel = countOnDutyForUnit(unit);

        return OperationsDashboardDto.UnitStatus.builder()
                .unitUid(unit.getUid())
                .unitName(unit.getName())
                .unitType(unit.getUnitType() != null ? unit.getUnitType().name() : null)
                .agencyType(unit.getAgency() != null ? unit.getAgency().getAgencyType() : null)
                .onDutyPersonnel(onDutyPersonnel)
                .activeIncidents(activeForUnit)
                .availableVehicles(availableVehicles)
                .build();
    }

    private int countOnDutyForUnit(EmergencyUnit unit) {
        long fire = fireOfficerRepository.findAll().stream()
                .filter(f -> Boolean.TRUE.equals(f.getIsOnDuty()) && Boolean.TRUE.equals(f.getIsActive())
                        && f.getEmergencyUnit() != null && unit.getUid().equals(f.getEmergencyUnit().getUid()))
                .count();
        long medic = medicRepository.findAll().stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsOnDuty()) && Boolean.TRUE.equals(m.getIsActive())
                        && m.getEmergencyUnit() != null && unit.getUid().equals(m.getEmergencyUnit().getUid()))
                .count();
        return (int) (fire + medic);
    }

    private long count(List<IncidentReport> list, EmergencyLevel level) {
        return list.stream().filter(i -> i.getEmergencyLevel() == level).count();
    }

    private long countStatus(List<IncidentReport> list, IncidentStatus status) {
        return list.stream().filter(i -> i.getStatus() == status).count();
    }

    private boolean isOnDuty(LocalTime start, LocalTime end, LocalTime now) {
        if (end.isAfter(start)) return !now.isBefore(start) && !now.isAfter(end);
        return !now.isBefore(start) || !now.isAfter(end);
    }
}
