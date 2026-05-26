package com.smartincident.incidentbackend.police.service;

import com.smartincident.incidentbackend.enums.AppointmentStatus;
import com.smartincident.incidentbackend.police.dto.AppointmentDto;
import com.smartincident.incidentbackend.police.entity.PoliceOfficer;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
import com.smartincident.incidentbackend.police.entity.StationAppointment;
import com.smartincident.incidentbackend.police.repository.PoliceStationRepository;
import com.smartincident.incidentbackend.police.repository.PoliceOfficerRepository;
import com.smartincident.incidentbackend.police.repository.StationAppointmentRepository;
import com.smartincident.incidentbackend.utils.PageableParam;
import com.smartincident.incidentbackend.utils.Response;
import com.smartincident.incidentbackend.utils.ResponseList;
import com.smartincident.incidentbackend.utils.ResponsePage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final StationAppointmentRepository appointmentRepository;
    private final PoliceOfficerRepository officerRepository;
    private final PoliceStationRepository policeStationRepository;

    @Transactional
    public Response<StationAppointment> saveAppointment(AppointmentDto dto) {
        if (dto == null) return Response.error("Appointment data is required");
        if (dto.getOfficerUid() == null) return Response.error("Officer is required");
        if (dto.getStationUid() == null) return Response.error("Station is required");
        if (dto.getPosition() == null) return Response.error("Position is required");
        if (dto.getStartDate() == null) return Response.error("Start date is required");

        StationAppointment appointment;

        if (dto.getUid() != null) {
            Optional<StationAppointment> existing = appointmentRepository.findByUid(dto.getUid());
            if (existing.isEmpty()) return Response.error("Appointment not found");
            appointment = existing.get();
        } else {
            appointment = new StationAppointment();
        }

        PoliceOfficer officer = officerRepository.findByUid(dto.getOfficerUid())
                .orElse(null);
        if (officer == null) return Response.error("Officer not found");

        PoliceStation station = policeStationRepository.findByUid(dto.getStationUid())
                .orElse(null);
        if (station == null) return Response.error("Police station not found");

        appointment.setOfficer(officer);
        appointment.setPoliceStation(station);
        appointment.setPosition(dto.getPosition());
        appointment.setStatus(dto.getStatus() != null ? dto.getStatus() : AppointmentStatus.ACTIVE);
        appointment.setStartDate(dto.getStartDate());
        appointment.setEndDate(dto.getEndDate());
        appointment.setNotes(dto.getNotes());

        if (dto.getUid() != null) appointment.update();

        try {
            StationAppointment saved = appointmentRepository.save(appointment);
            return Response.success(saved);
        } catch (Exception e) {
            log.error("Failed to save appointment: {}", e.getMessage());
            return Response.error("Failed to save appointment: " + e.getMessage());
        }
    }

    public ResponsePage<StationAppointment> getByStation(PageableParam pageableParam, String stationUid, AppointmentStatus status) {
        return new ResponsePage<>(appointmentRepository.getByStation(
                pageableParam.getPageable(false), stationUid, status));
    }

    public ResponseList<StationAppointment> getByOfficer(String officerUid) {
        List<StationAppointment> list = appointmentRepository.findByOfficerUid(officerUid);
        return new ResponseList<>(list);
    }

    public ResponseList<StationAppointment> getActiveByStation(String stationUid) {
        List<StationAppointment> list = appointmentRepository.findByStationUidOrderByPosition(stationUid);
        return new ResponseList<>(list);
    }

    @Transactional
    public Response<StationAppointment> updateStatus(String uid, AppointmentStatus status) {
        Optional<StationAppointment> opt = appointmentRepository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Appointment not found");
        StationAppointment appt = opt.get();
        appt.setStatus(status);
        if (status == AppointmentStatus.TRANSFERRED || status == AppointmentStatus.RETIRED) {
            appt.setEndDate(LocalDate.now());
        }
        appt.update();
        appointmentRepository.save(appt);
        return Response.success(appt);
    }

    @Transactional
    public Response<StationAppointment> deleteAppointment(String uid) {
        Optional<StationAppointment> opt = appointmentRepository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Appointment not found");
        StationAppointment appt = opt.get();
        appt.delete();
        appointmentRepository.save(appt);
        return Response.success(appt);
    }
}
