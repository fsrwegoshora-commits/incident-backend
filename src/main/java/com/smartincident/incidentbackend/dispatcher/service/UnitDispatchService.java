package com.smartincident.incidentbackend.dispatcher.service;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.dispatcher.dto.UnitDispatchRequest;
import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.emergency.repository.EmergencyUnitRepository;
import com.smartincident.incidentbackend.enums.*;
import com.smartincident.incidentbackend.fire.entity.FireOfficer;
import com.smartincident.incidentbackend.fire.repository.FireOfficerRepository;
import com.smartincident.incidentbackend.incident.entity.IncidentAgency;
import com.smartincident.incidentbackend.incident.entity.IncidentReport;
import com.smartincident.incidentbackend.incident.repository.IncidentAgencyRepository;
import com.smartincident.incidentbackend.incident.repository.IncidentReportRepository;
import com.smartincident.incidentbackend.medical.entity.Medic;
import com.smartincident.incidentbackend.medical.repository.MedicRepository;
import com.smartincident.incidentbackend.notification.dto.NotificationDto;
import com.smartincident.incidentbackend.notification.service.NotificationService;
import com.smartincident.incidentbackend.police.entity.OfficerShift;
import com.smartincident.incidentbackend.police.entity.PoliceOfficer;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
import com.smartincident.incidentbackend.police.repository.OfficerShiftRepository;
import com.smartincident.incidentbackend.police.repository.PoliceOfficerRepository;
import com.smartincident.incidentbackend.police.repository.PoliceStationRepository;
import com.smartincident.incidentbackend.setting.entity.Agency;
import com.smartincident.incidentbackend.setting.repository.AgencyRepository;
import com.smartincident.incidentbackend.utils.LoggedUser;
import com.smartincident.incidentbackend.utils.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnitDispatchService {

    private final IncidentReportRepository incidentRepository;
    private final PoliceStationRepository policeStationRepository;
    private final EmergencyUnitRepository emergencyUnitRepository;
    private final OfficerShiftRepository officerShiftRepository;
    private final PoliceOfficerRepository policeOfficerRepository;
    private final FireOfficerRepository fireOfficerRepository;
    private final MedicRepository medicRepository;
    private final IncidentAgencyRepository incidentAgencyRepository;
    private final AgencyRepository agencyRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Transactional
    public Response<IncidentReport> dispatchUnits(String incidentUid, UnitDispatchRequest req) {
        if (incidentUid == null) return Response.error("Incident UID is required");
        if (req == null) return Response.error("Dispatch request is required");

        Optional<IncidentReport> incOpt = incidentRepository.findByUid(incidentUid);
        if (incOpt.isEmpty()) return Response.error("Incident not found");
        IncidentReport incident = incOpt.get();

        if (incident.getStatus() == IncidentStatus.RESOLVED || incident.getStatus() == IncidentStatus.CLOSED)
            return Response.error("Cannot dispatch to a resolved or closed incident");

        String dispatcherUid = LoggedUser.getUid();
        User dispatcher = userRepository.findByUid(dispatcherUid).orElse(null);

        List<String> responderUids = new ArrayList<>();

        // ── Police Station dispatch ───────────────────────────────────────────
        if (req.getPoliceStationUid() != null) {
            PoliceStation station = policeStationRepository.findByUid(req.getPoliceStationUid()).orElse(null);
            if (station == null) return Response.error("Police station not found: " + req.getPoliceStationUid());

            incident.setAssignedPoliceStation(station);

            // Auto-assign on-duty officers from this station today
            List<OfficerShift> onDutyShifts = officerShiftRepository
                    .findByStationAndDate(station.getUid(), LocalDate.now())
                    .stream()
                    .filter(s -> isCurrentlyOnDuty(s.getStartTime(), s.getEndTime()))
                    .toList();

            if (!onDutyShifts.isEmpty()) {
                PoliceOfficer leadOfficer = onDutyShifts.get(0).getOfficer();
                incident.setAssignedOfficer(leadOfficer);
                responderUids.addAll(onDutyShifts.stream()
                        .map(s -> s.getOfficer().getUserAccount().getUid())
                        .collect(Collectors.toList()));
                log.info("Auto-assigned {} officers from station {}", onDutyShifts.size(), station.getName());
            } else {
                // Fall back: assign any active officer from the station
                List<PoliceOfficer> officers = policeOfficerRepository.findByPoliceStationUidAndIsActiveTrue(station.getUid());
                if (!officers.isEmpty()) {
                    incident.setAssignedOfficer(officers.get(0));
                    responderUids.add(officers.get(0).getUserAccount().getUid());
                }
            }

            ensureAgencyRecord(incident, "POLICE", AgencyRole.LEAD);
        }

        // ── Fire Unit dispatch ────────────────────────────────────────────────
        if (req.getFireUnitUid() != null) {
            EmergencyUnit fireUnit = emergencyUnitRepository.findByUid(req.getFireUnitUid()).orElse(null);
            if (fireUnit == null) return Response.error("Fire unit not found: " + req.getFireUnitUid());

            incident.setAssignedUnit(fireUnit);

            List<FireOfficer> fireOfficers = fireOfficerRepository.findByUnitUidOrderedByRank(fireUnit.getUid());
            responderUids.addAll(fireOfficers.stream()
                    .filter(f -> Boolean.TRUE.equals(f.getIsOnDuty()))
                    .map(f -> f.getUserAccount().getUid())
                    .collect(Collectors.toList()));
            log.info("Dispatching fire unit {} with {} on-duty officers", fireUnit.getName(), responderUids.size());

            ensureAgencyRecord(incident, "FIRE",
                    incident.getRequiresPoliceService() ? AgencyRole.SUPPORT : AgencyRole.LEAD);
        }

        // ── Medical Unit dispatch ─────────────────────────────────────────────
        if (req.getMedicalUnitUid() != null) {
            EmergencyUnit medUnit = emergencyUnitRepository.findByUid(req.getMedicalUnitUid()).orElse(null);
            if (medUnit == null) return Response.error("Medical unit not found: " + req.getMedicalUnitUid());

            incident.setAssignedUnit(medUnit);

            List<Medic> medics = medicRepository.findByEmergencyUnitUidAndIsActiveTrue(medUnit.getUid());
            responderUids.addAll(medics.stream()
                    .filter(m -> Boolean.TRUE.equals(m.getIsOnDuty()))
                    .map(m -> m.getUserAccount().getUid())
                    .collect(Collectors.toList()));
            log.info("Dispatching medical unit {} with {} on-duty medics", medUnit.getName(), responderUids.size());

            ensureAgencyRecord(incident, "MEDICAL",
                    incident.getRequiresPoliceService() || incident.getRequiresFireService()
                            ? AgencyRole.SUPPORT : AgencyRole.LEAD);
        }

        incident.setStatus(IncidentStatus.DISPATCHED);
        incident.update();

        try {
            incident = incidentRepository.save(incident);
            notifyResponders(incident, responderUids);
            notifyDispatcher(incident, dispatcher);
            log.info("Units dispatched to incident {} — {} responders notified", incidentUid, responderUids.size());
            return Response.success(incident);
        } catch (Exception e) {
            log.error("Failed to dispatch units: {}", e.getMessage());
            return Response.error("Dispatch failed: " + e.getMessage());
        }
    }

    /** Returns all currently active (DISPATCHED / IN_PROGRESS) incidents. */
    public Response<List<IncidentReport>> getOperationalIncidents() {
        return Response.success(incidentRepository.findOperationalIncidents());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private boolean isCurrentlyOnDuty(LocalTime start, LocalTime end) {
        LocalTime now = LocalTime.now();
        if (end.isAfter(start)) return !now.isBefore(start) && !now.isAfter(end);
        // Overnight shift
        return !now.isBefore(start) || !now.isAfter(end);
    }

    private void ensureAgencyRecord(IncidentReport incident, String agencyCode, AgencyRole role) {
        agencyRepository.findByCode(agencyCode).ifPresent(obj -> {
            Agency agency = (Agency) obj;
            boolean exists = incidentAgencyRepository
                    .findByIncidentUidAndAgencyUid(incident.getUid(), agency.getUid()).isPresent();
            if (!exists) {
                IncidentAgency ia = new IncidentAgency();
                ia.setIncident(incident);
                ia.setAgency(agency);
                ia.setAgencyRole(role);
                ia.setResponseStatus(AgencyResponseStatus.RESPONDING);
                ia.setNotifiedAt(LocalDateTime.now());
                ia.setRespondedAt(LocalDateTime.now());
                incidentAgencyRepository.save(ia);
            } else {
                incidentAgencyRepository.findByIncidentUidAndAgencyUid(incident.getUid(), agency.getUid())
                        .ifPresent(ia -> {
                            ia.setResponseStatus(AgencyResponseStatus.RESPONDING);
                            ia.setRespondedAt(LocalDateTime.now());
                            incidentAgencyRepository.save(ia);
                        });
            }
        });
    }

    private void notifyResponders(IncidentReport incident, List<String> uids) {
        if (uids.isEmpty()) return;
        NotificationDto dto = new NotificationDto();
        dto.setTitle("You Have Been Dispatched");
        dto.setMessage("You are dispatched to: " + incident.getTitle()
                + " at " + incident.getLocation() + " [" + incident.getEmergencyLevel() + "]");
        dto.setType(NotificationType.INCIDENT_ASSIGNED);
        dto.setChannels(List.of(NotificationChannel.IN_APP, NotificationChannel.PUSH));
        dto.setRelatedEntityUid(incident.getUid());
        dto.setRelatedEntityType("INCIDENT");
        dto.setTargetUserUids(uids);
        notificationService.sendNotification(dto);
    }

    private void notifyDispatcher(IncidentReport incident, User dispatcher) {
        if (dispatcher == null) return;
        NotificationDto dto = new NotificationDto();
        dto.setTitle("Units Dispatched");
        dto.setMessage("Units have been dispatched to: " + incident.getTitle());
        dto.setType(NotificationType.INCIDENT_ASSIGNED);
        dto.setChannels(List.of(NotificationChannel.IN_APP));
        dto.setRelatedEntityUid(incident.getUid());
        dto.setRelatedEntityType("INCIDENT");
        dto.setTargetUserUids(List.of(dispatcher.getUid()));
        notificationService.sendNotification(dto);
    }
}
