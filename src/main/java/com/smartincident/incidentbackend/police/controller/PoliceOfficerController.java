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
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
@GraphQLApi
@Component
public class PoliceOfficerController {
    private final PoliceOfficerService policeOfficerService;
    private final PoliceOfficerRepository policeOfficerRepository;
    private final OfficerShiftRepository officerShiftRepository;

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN,Role.ROOT})
    @GraphQLMutation(name = "savePoliceOfficer", description = "New police officer")
    public Response<PoliceOfficer> savePoliceOfficer(@GraphQLArgument(name = "policeOfficerDto") PoliceOfficerDto policeOfficerDto) {
        return policeOfficerService.savePoliceOfficer(policeOfficerDto);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN,Role.ROOT})
    @GraphQLQuery(name = "getPoliceOfficer", description = "Gets a police police using it's uid")
    public Response<PoliceOfficer> getPoliceOfficer(@GraphQLArgument(name = "uid") String uid) {
        return policeOfficerService.getPoliceOfficer(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN,Role.ROOT})
    @GraphQLMutation(name = "deletePoliceOfficer", description = "Deletes a police Officer using it's uid")
    public Response<PoliceOfficer> deletePoliceOfficer(@GraphQLArgument(name = "uid") String uid) {
        return policeOfficerService.deletePoliceOfficer(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.ROOT})
    @GraphQLQuery(name = "getPoliceOfficers", description = "Gets a page of police officer")
    public ResponsePage<PoliceOfficer> getPoliceOfficers(@GraphQLArgument(name = "pageableParam") PageableParam pageableParam) {
        return policeOfficerService.getPoliceOfficers(pageableParam != null ? pageableParam : new PageableParam());
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN,Role.ROOT})
    @GraphQLQuery(name = "getPoliceOfficersByStation", description = "Gets a page of police officer")
    public ResponsePage<PoliceOfficer> getPoliceOfficersByStation(@GraphQLArgument(name = "pageableParam") PageableParam pageableParam, @GraphQLArgument(name = "policeStationUid") String policeStationUid) {
        return policeOfficerService.getPoliceOfficersByStation(pageableParam != null ? pageableParam : new PageableParam(), policeStationUid);
    }
    @GraphQLQuery(name = "getAvailableOfficersForDate")
    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    public ResponseList<PoliceOfficer> getAvailableOfficersForDate(@GraphQLArgument(name = "date") LocalDate date) {
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
    @GraphQLQuery(name = "getAvailableOfficersForSlot")
    public ResponseList<PoliceOfficer> getAvailableOfficersForSlot(
            @GraphQLArgument(name = "date") String date,
            @GraphQLArgument(name = "startTime") String startTime,
            @GraphQLArgument(name = "endTime") String endTime
    ) {
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
