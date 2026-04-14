package com.smartincident.incidentbackend.police.controller;

import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.police.dto.PoliceOfficerDto;
import com.smartincident.incidentbackend.police.entity.OfficerShift;
import com.smartincident.incidentbackend.police.entity.PoliceOfficer;
import com.smartincident.incidentbackend.police.repository.OfficerShiftRepository;
import com.smartincident.incidentbackend.police.repository.PoliceOfficerRepository;
import com.smartincident.incidentbackend.police.service.PoliceOfficerService;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/police/officers")
@RequiredArgsConstructor
public class PoliceOfficerController {
    private final PoliceOfficerService policeOfficerService;
    private final PoliceOfficerRepository policeOfficerRepository;
    private final OfficerShiftRepository officerShiftRepository;

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

        List<PoliceOfficer> allOfficers = policeOfficerRepository.findByStationUidAndIsActiveTrue(stationUid);
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
            @RequestParam String endTime) {
        if (date == null || startTime == null || endTime == null) {
            return ResponseList.error("Date and time range are required");
        }

        LocalDate parsedDate = LocalDate.parse(date);
        LocalTime parsedStart = LocalTime.parse(startTime);
        LocalTime parsedEnd = LocalTime.parse(endTime);

        String stationUid = LoggedUser.getStationUid();
        if (stationUid == null) return ResponseList.error("Station context missing");

        List<PoliceOfficer> allOfficers = policeOfficerRepository.findByStationUidAndIsActiveTrue(stationUid);
        List<PoliceOfficer> available = new ArrayList<>();

        for (PoliceOfficer officer : allOfficers) {
            List<OfficerShift> shifts = officerShiftRepository.findByOfficerUidAndShiftDate(officer.getUid(), parsedDate);

            boolean overlaps = shifts.stream().anyMatch(shift ->
                    !(shift.getEndTime().isBefore(parsedStart) || shift.getStartTime().isAfter(parsedEnd))
            );

            if (!overlaps) {
                available.add(officer);
            }
        }

        return new ResponseList<>(available);
    }
}
