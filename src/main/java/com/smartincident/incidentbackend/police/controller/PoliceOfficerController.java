package com.smartincident.incidentbackend.police.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.enums.AppointmentPosition;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.police.dto.PoliceOfficerDto;
import com.smartincident.incidentbackend.police.entity.OfficerShift;
import com.smartincident.incidentbackend.police.entity.PoliceOfficer;
import com.smartincident.incidentbackend.police.repository.OfficerShiftRepository;
import com.smartincident.incidentbackend.police.repository.PoliceOfficerRepository;
import com.smartincident.incidentbackend.police.repository.StationAppointmentRepository;
import com.smartincident.incidentbackend.police.repository.TrafficCheckPointRepository;
import com.smartincident.incidentbackend.police.service.PoliceOfficerService;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/police/officers")
@RequiredArgsConstructor
public class PoliceOfficerController {
    private final PoliceOfficerService policeOfficerService;
    private final PoliceOfficerRepository policeOfficerRepository;
    private final OfficerShiftRepository officerShiftRepository;
    private final StationAppointmentRepository appointmentRepository;
    private final TrafficCheckPointRepository checkpointRepository;

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @PostMapping
    public Response<PoliceOfficer> savePoliceOfficer(@RequestBody PoliceOfficerDto policeOfficerDto) {
        return policeOfficerService.savePoliceOfficer(policeOfficerDto);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @GetMapping("/{uid}")
    public Response<PoliceOfficer> getPoliceOfficer(@PathVariable String uid) {
        return policeOfficerService.getPoliceOfficer(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @DeleteMapping("/{uid}")
    public Response<PoliceOfficer> deletePoliceOfficer(@PathVariable String uid) {
        return policeOfficerService.deletePoliceOfficer(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.ROOT})
    @GetMapping
    public ResponsePage<PoliceOfficer> getPoliceOfficers(@ModelAttribute PageableParam pageableParam) {
        return policeOfficerService.getPoliceOfficers(pageableParam != null ? pageableParam : new PageableParam());
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @GetMapping("/by-station/{policeStationUid}")
    public ResponsePage<PoliceOfficer> getPoliceOfficersByStation(
            @ModelAttribute PageableParam pageableParam,
            @PathVariable String policeStationUid) {
        return policeOfficerService.getPoliceOfficersByStation(pageableParam != null ? pageableParam : new PageableParam(), policeStationUid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @GetMapping("/available/date")
    public ResponseList<PoliceOfficer> getAvailableOfficersForDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) return ResponseList.error("Date is required");

        String stationUid = LoggedUser.getStationUid();
        if (stationUid == null) return ResponseList.error("Station context missing");

        List<PoliceOfficer> allOfficers = policeOfficerRepository.findByPoliceStationUidAndIsActiveTrue(stationUid);
        List<PoliceOfficer> available = new ArrayList<>();

        for (PoliceOfficer officer : allOfficers) {
            boolean hasShiftToday = officerShiftRepository.existsByOfficerUidAndShiftDate(officer.getUid(), date);
            boolean hasShiftBefore = officerShiftRepository.existsByOfficerUidAndShiftDate(officer.getUid(), date.minusDays(1));
            boolean hasShiftAfter = officerShiftRepository.existsByOfficerUidAndShiftDate(officer.getUid(), date.plusDays(1));

            if (!hasShiftToday && !hasShiftBefore && !hasShiftAfter) {
                available.add(officer);
            }
        }

        return new ResponseList<>(available);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    @GetMapping("/available/slot")
    public ResponseList<PoliceOfficer> getAvailableOfficersForSlot(
            @RequestParam String date,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(required = false) String dutyType) {

        if (date == null || startTime == null || endTime == null)
            return ResponseList.error("Date and time range are required");

        LocalDate parsedDate  = LocalDate.parse(date);
        LocalTime parsedStart = LocalTime.parse(startTime);
        LocalTime parsedEnd   = LocalTime.parse(endTime);

        String stationUid = LoggedUser.getStationUid();
        if (stationUid == null) return ResponseList.error("Station context missing");

        List<PoliceOfficer> allOfficers = policeOfficerRepository.findByPoliceStationUidAndIsActiveTrue(stationUid);

        // ── Build exclusion set ────────────────────────────────────────────────
        Set<String> excludedUids = new HashSet<>();

        boolean isCheckpointDuty = "CHECKPOINT_DUTY".equals(dutyType);
        boolean isStationDuty    = "STATION_DUTY".equals(dutyType);

        if (isCheckpointDuty || isStationDuty) {
            // Both duty types: exclude officers already serving as checkpoint supervisors
            excludedUids.addAll(checkpointRepository.findSupervisorOfficerUidsByStation(stationUid));
        }

        if (isStationDuty) {
            // Station Duty: exclude traffic officers and management/administrative positions
            List<AppointmentPosition> stationExcluded = List.of(
                    AppointmentPosition.TRAFFIC_OFFICER,
                    AppointmentPosition.OFFICER_IN_CHARGE,
                    AppointmentPosition.DEPUTY_OFFICER_IN_CHARGE,
                    AppointmentPosition.ADMINISTRATIVE_OFFICER
            );
            excludedUids.addAll(appointmentRepository.findOfficerUidsByStationAndPositions(stationUid, stationExcluded));

            // Also exclude users with STATION_ADMIN role
            allOfficers = allOfficers.stream()
                    .filter(o -> o.getUserAccount() == null ||
                                 o.getUserAccount().getRole() != Role.STATION_ADMIN)
                    .toList();
        }

        // ── Time-overlap filter + exclusion set filter ─────────────────────────
        List<PoliceOfficer> available = new ArrayList<>();
        for (PoliceOfficer officer : allOfficers) {
            if (excludedUids.contains(officer.getUid())) continue;

            List<OfficerShift> shifts = officerShiftRepository
                    .findByOfficerUidAndShiftDate(officer.getUid(), parsedDate);

            boolean overlaps = shifts.stream().anyMatch(s ->
                    !(s.getEndTime().isBefore(parsedStart) || s.getStartTime().isAfter(parsedEnd)));

            if (!overlaps) available.add(officer);
        }

        return new ResponseList<>(available);
    }
}
