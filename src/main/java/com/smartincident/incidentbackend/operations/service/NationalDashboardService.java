package com.smartincident.incidentbackend.operations.service;

import com.smartincident.incidentbackend.dispatcher.repository.DispatcherShiftRepository;
import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.emergency.repository.EmergencyUnitRepository;
import com.smartincident.incidentbackend.emergency.repository.EmergencyVehicleRepository;
import com.smartincident.incidentbackend.enums.*;
import com.smartincident.incidentbackend.fire.repository.FireOfficerRepository;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.incident.repository.IncidentReportRepository;
import com.smartincident.incidentbackend.medical.repository.MedicRepository;
import com.smartincident.incidentbackend.operations.dto.NationalDashboardDto;
import com.smartincident.incidentbackend.police.repository.OfficerShiftRepository;
import com.smartincident.incidentbackend.setting.entity.Agency;
import com.smartincident.incidentbackend.setting.repository.AgencyRepository;
import com.smartincident.incidentbackend.utils.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NationalDashboardService {

    private final IncidentReportRepository incidentRepository;
    private final AgencyRepository agencyRepository;
    private final EmergencyUnitRepository emergencyUnitRepository;
    private final EmergencyVehicleRepository vehicleRepository;
    private final DispatcherShiftRepository dispatcherShiftRepository;
    private final OfficerShiftRepository officerShiftRepository;
    private final FireOfficerRepository fireOfficerRepository;
    private final MedicRepository medicRepository;

    private static final List<IncidentStatus> ACTIVE = List.of(
            IncidentStatus.REPORTED, IncidentStatus.UNDER_REVIEW, IncidentStatus.CLASSIFIED,
            IncidentStatus.WAITING_FOR_DISPATCH, IncidentStatus.DISPATCHED,
            IncidentStatus.ACKNOWLEDGED, IncidentStatus.EN_ROUTE,
            IncidentStatus.AT_SCENE, IncidentStatus.IN_PROGRESS);

    public Response<NationalDashboardDto> getNationalDashboard() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        LocalDateTime yesterday = LocalDateTime.now().minusHours(24);

        List<IncidentReport> all = incidentRepository.findAll();
        List<IncidentReport> active = all.stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsActive()) && ACTIVE.contains(i.getStatus()))
                .toList();

        long critical   = active.stream().filter(i -> i.getEmergencyLevel() == EmergencyLevel.CRITICAL).count();
        long slaBreached = active.stream().filter(i -> i.getSlaBreachedAt() != null).count();
        long major      = active.stream().filter(i -> Boolean.TRUE.equals(i.getIsMajorIncident())).count();
        long eoc        = active.stream().filter(i -> Boolean.TRUE.equals(i.getEocActivated())).count();

        // Status breakdown
        Map<String, Long> byStatus = Arrays.stream(IncidentStatus.values())
                .collect(Collectors.toMap(Enum::name,
                        s -> active.stream().filter(i -> i.getStatus() == s).count(),
                        (a, b) -> a, LinkedHashMap::new));
        byStatus.values().removeIf(v -> v == 0);

        // Level breakdown
        Map<String, Long> byLevel = Arrays.stream(EmergencyLevel.values())
                .collect(Collectors.toMap(Enum::name,
                        l -> active.stream().filter(i -> i.getEmergencyLevel() == l).count(),
                        (a, b) -> a, LinkedHashMap::new));
        byLevel.values().removeIf(v -> v == 0);

        // Type breakdown
        Map<String, Long> byType = active.stream()
                .filter(i -> i.getType() != null)
                .collect(Collectors.groupingBy(i -> i.getType().name(), Collectors.counting()));

        // On-duty counts
        long onDutyDispatch = dispatcherShiftRepository.findOnDutyNow(today, now).size();
        long onDutyPolice = officerShiftRepository.findByDate(today).stream()
                .filter(s -> isOnDuty(s.getStartTime(), s.getEndTime(), now)).count();
        long onDutyFire = fireOfficerRepository.findAll().stream()
                .filter(f -> Boolean.TRUE.equals(f.getIsOnDuty()) && Boolean.TRUE.equals(f.getIsActive())).count();
        long onDutyMedic = medicRepository.findAll().stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsOnDuty()) && Boolean.TRUE.equals(m.getIsActive())).count();
        long totalPersonnel = onDutyDispatch + onDutyPolice + onDutyFire + onDutyMedic;

        long totalActiveVehicles = vehicleRepository.findAll().stream()
                .filter(v -> v.getStatus() == VehicleStatus.DISPATCHED
                        || v.getStatus() == VehicleStatus.EN_ROUTE
                        || v.getStatus() == VehicleStatus.ON_SCENE)
                .count();

        // Per-agency breakdown
        List<NationalDashboardDto.AgencyStatus> agencyStatuses = agencyRepository.findAll().stream()
                .map(agency -> buildAgencyStatus(agency, active))
                .toList();

        // Regional summary based on assigned emergency unit's administrative area
        List<NationalDashboardDto.RegionalSummary> regional = buildRegionalSummary(active, all, yesterday);

        return Response.success(NationalDashboardDto.builder()
                .totalActiveIncidents((long) active.size())
                .totalActiveVehicles(totalActiveVehicles)
                .totalOnDutyPersonnel(totalPersonnel)
                .criticalIncidents(critical)
                .slaBreached(slaBreached)
                .majorIncidents(major)
                .eocActivated(eoc)
                .incidentsByStatus(byStatus)
                .incidentsByLevel(byLevel)
                .incidentsByType(byType)
                .agencyStatuses(agencyStatuses)
                .regionalSummary(regional)
                .build());
    }

    private NationalDashboardDto.AgencyStatus buildAgencyStatus(Agency agency, List<IncidentReport> active) {
        List<IncidentReport> forAgency = active.stream()
                .filter(i -> i.getLeadAgency() != null && agency.getUid().equals(i.getLeadAgency().getUid()))
                .toList();

        long agencyCritical  = forAgency.stream().filter(i -> i.getEmergencyLevel() == EmergencyLevel.CRITICAL).count();
        long agencySla       = forAgency.stream().filter(i -> i.getSlaBreachedAt() != null).count();

        int onDutyFire = (int) fireOfficerRepository.findAll().stream()
                .filter(f -> Boolean.TRUE.equals(f.getIsOnDuty()) && Boolean.TRUE.equals(f.getIsActive())
                        && f.getEmergencyUnit() != null
                        && f.getEmergencyUnit().getAgency() != null
                        && agency.getUid().equals(f.getEmergencyUnit().getAgency().getUid()))
                .count();
        int onDutyMedic = (int) medicRepository.findAll().stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsOnDuty()) && Boolean.TRUE.equals(m.getIsActive())
                        && m.getEmergencyUnit() != null
                        && m.getEmergencyUnit().getAgency() != null
                        && agency.getUid().equals(m.getEmergencyUnit().getAgency().getUid()))
                .count();

        List<EmergencyUnit> agencyUnits = emergencyUnitRepository.findAll().stream()
                .filter(u -> u.getAgency() != null && agency.getUid().equals(u.getAgency().getUid()))
                .toList();

        int available  = agencyUnits.stream()
                .mapToInt(u -> vehicleRepository.findByUnitUidAndStatus(u.getUid(), VehicleStatus.AVAILABLE).size())
                .sum();
        int dispatched = agencyUnits.stream()
                .mapToInt(u -> vehicleRepository.findByUnitUidAndStatus(u.getUid(), VehicleStatus.DISPATCHED).size())
                .sum();

        return NationalDashboardDto.AgencyStatus.builder()
                .agencyUid(agency.getUid())
                .agencyName(agency.getName())
                .agencyType(agency.getAgencyType())
                .activeIncidents((long) forAgency.size())
                .criticalIncidents(agencyCritical)
                .slaBreached(agencySla)
                .onDutyPersonnel(onDutyFire + onDutyMedic)
                .availableVehicles(available)
                .dispatchedVehicles(dispatched)
                .build();
    }

    private List<NationalDashboardDto.RegionalSummary> buildRegionalSummary(
            List<IncidentReport> active, List<IncidentReport> all, LocalDateTime yesterday) {

        // Group by administrative area name via assigned unit or police station
        Map<String, List<IncidentReport>> byRegion = active.stream()
                .collect(Collectors.groupingBy(i -> resolveRegion(i)));

        Map<String, Long> resolved24h = all.stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsActive())
                        && i.getResolvedAt() != null && i.getResolvedAt().isAfter(yesterday))
                .collect(Collectors.groupingBy(i -> resolveRegion(i), Collectors.counting()));

        return byRegion.entrySet().stream()
                .map(e -> NationalDashboardDto.RegionalSummary.builder()
                        .region(e.getKey())
                        .activeIncidents((long) e.getValue().size())
                        .critical(e.getValue().stream().filter(i -> i.getEmergencyLevel() == EmergencyLevel.CRITICAL).count())
                        .resolved24h(resolved24h.getOrDefault(e.getKey(), 0L))
                        .build())
                .sorted(Comparator.comparingLong(NationalDashboardDto.RegionalSummary::getActiveIncidents).reversed())
                .toList();
    }

    private String resolveRegion(IncidentReport i) {
        if (i.getAssignedUnit() != null && i.getAssignedUnit().getAdministrativeArea() != null
                && i.getAssignedUnit().getAdministrativeArea().getName() != null) {
            return i.getAssignedUnit().getAdministrativeArea().getName();
        }
        if (i.getAssignedPoliceStation() != null
                && i.getAssignedPoliceStation().getLocation() != null
                && i.getAssignedPoliceStation().getLocation().getAddress() != null) {
            return i.getAssignedPoliceStation().getLocation().getAddress();
        }
        return "Unknown Region";
    }

    private boolean isOnDuty(LocalTime start, LocalTime end, LocalTime now) {
        if (end.isAfter(start)) return !now.isBefore(start) && !now.isAfter(end);
        return !now.isBefore(start) || !now.isAfter(end);
    }
}
