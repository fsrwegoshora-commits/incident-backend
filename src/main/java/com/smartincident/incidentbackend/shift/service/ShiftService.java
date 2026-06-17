package com.smartincident.incidentbackend.shift.service;

import com.smartincident.incidentbackend.dispatcher.entity.DispatcherShift;
import com.smartincident.incidentbackend.dispatcher.repository.DispatcherShiftRepository;
import com.smartincident.incidentbackend.fire.entity.FireOfficer;
import com.smartincident.incidentbackend.fire.repository.FireOfficerRepository;
import com.smartincident.incidentbackend.medical.entity.Medic;
import com.smartincident.incidentbackend.medical.repository.MedicRepository;
import com.smartincident.incidentbackend.police.entity.OfficerShift;
import com.smartincident.incidentbackend.police.repository.OfficerShiftRepository;
import com.smartincident.incidentbackend.shift.dto.OnDutyPersonnelDto;
import com.smartincident.incidentbackend.utils.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShiftService {

    private final DispatcherShiftRepository dispatcherShiftRepository;
    private final OfficerShiftRepository officerShiftRepository;
    private final FireOfficerRepository fireOfficerRepository;
    private final MedicRepository medicRepository;

    public Response<OnDutyPersonnelDto> getOnDutyPersonnel() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<OnDutyPersonnelDto.PersonnelRecord> dispatchers = dispatcherShiftRepository
                .findOnDutyNow(today, now)
                .stream()
                .map(s -> toDispatcherRecord(s))
                .toList();

        List<OnDutyPersonnelDto.PersonnelRecord> policeOfficers = officerShiftRepository
                .findByDate(today)
                .stream()
                .filter(s -> isCurrentlyOnDuty(s.getStartTime(), s.getEndTime(), now))
                .map(s -> toPoliceRecord(s))
                .toList();

        List<OnDutyPersonnelDto.PersonnelRecord> fireOfficers = fireOfficerRepository
                .findAll()
                .stream()
                .filter(f -> Boolean.TRUE.equals(f.getIsOnDuty()) && Boolean.TRUE.equals(f.getIsActive()))
                .map(f -> toFireRecord(f))
                .toList();

        List<OnDutyPersonnelDto.PersonnelRecord> medics = medicRepository
                .findAll()
                .stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsOnDuty()) && Boolean.TRUE.equals(m.getIsActive()))
                .map(m -> toMedicRecord(m))
                .toList();

        int total = dispatchers.size() + policeOfficers.size() + fireOfficers.size() + medics.size();

        return Response.success(OnDutyPersonnelDto.builder()
                .totalOnDuty(total)
                .dispatchers(dispatchers)
                .policeOfficers(policeOfficers)
                .fireOfficers(fireOfficers)
                .medics(medics)
                .build());
    }

    private boolean isCurrentlyOnDuty(LocalTime start, LocalTime end, LocalTime now) {
        if (end.isAfter(start)) return !now.isBefore(start) && !now.isAfter(end);
        return !now.isBefore(start) || !now.isAfter(end);
    }

    private OnDutyPersonnelDto.PersonnelRecord toDispatcherRecord(DispatcherShift s) {
        return OnDutyPersonnelDto.PersonnelRecord.builder()
                .uid(s.getDispatcher().getUid())
                .name(s.getDispatcher().getName())
                .phoneNumber(s.getDispatcher().getPhoneNumber())
                .role(s.getDispatcher().getRole().name())
                .unit(s.getDispatcher().getEmergencyUnit() != null ? s.getDispatcher().getEmergencyUnit().getName() : null)
                .shiftDate(s.getShiftDate())
                .shiftStart(s.getStartTime())
                .shiftEnd(s.getEndTime())
                .build();
    }

    private OnDutyPersonnelDto.PersonnelRecord toPoliceRecord(OfficerShift s) {
        return OnDutyPersonnelDto.PersonnelRecord.builder()
                .uid(s.getOfficer().getUserAccount().getUid())
                .name(s.getOfficer().getUserAccount().getName())
                .phoneNumber(s.getOfficer().getUserAccount().getPhoneNumber())
                .role("POLICE_OFFICER")
                .station(s.getOfficer().getPoliceStation() != null ? s.getOfficer().getPoliceStation().getName() : null)
                .badgeOrServiceNumber(s.getOfficer().getBadgeNumber())
                .shiftDate(s.getShiftDate())
                .shiftStart(s.getStartTime())
                .shiftEnd(s.getEndTime())
                .build();
    }

    private OnDutyPersonnelDto.PersonnelRecord toFireRecord(FireOfficer f) {
        return OnDutyPersonnelDto.PersonnelRecord.builder()
                .uid(f.getUserAccount().getUid())
                .name(f.getUserAccount().getName())
                .phoneNumber(f.getUserAccount().getPhoneNumber())
                .role("FIRE_OFFICER")
                .unit(f.getEmergencyUnit() != null ? f.getEmergencyUnit().getName() : null)
                .badgeOrServiceNumber(f.getServiceNumber())
                .build();
    }

    private OnDutyPersonnelDto.PersonnelRecord toMedicRecord(Medic m) {
        return OnDutyPersonnelDto.PersonnelRecord.builder()
                .uid(m.getUserAccount().getUid())
                .name(m.getUserAccount().getName())
                .phoneNumber(m.getUserAccount().getPhoneNumber())
                .role("MEDIC")
                .unit(m.getEmergencyUnit() != null ? m.getEmergencyUnit().getName() : null)
                .badgeOrServiceNumber(m.getStaffNumber())
                .build();
    }
}
