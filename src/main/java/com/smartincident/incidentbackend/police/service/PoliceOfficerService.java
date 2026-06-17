package com.smartincident.incidentbackend.police.service;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.enums.AppointmentStatus;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.police.dto.OfficerProfileUpdateDto;
import com.smartincident.incidentbackend.police.dto.PoliceOfficerDto;
import com.smartincident.incidentbackend.police.dto.PoliceOfficerResponse;
import com.smartincident.incidentbackend.police.entity.PoliceOfficer;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
import com.smartincident.incidentbackend.police.entity.StationAppointment;
import com.smartincident.incidentbackend.police.repository.OfficerShiftRepository;
import com.smartincident.incidentbackend.police.repository.PoliceOfficerRepository;
import com.smartincident.incidentbackend.police.repository.PoliceStationRepository;
import com.smartincident.incidentbackend.police.repository.StationAppointmentRepository;
import com.smartincident.incidentbackend.setting.entity.Department;
import com.smartincident.incidentbackend.setting.repository.DepartmentRepository;
import com.smartincident.incidentbackend.utils.*;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@GraphQLApi
public class PoliceOfficerService {

    private final DepartmentRepository departmentRepository;
    private final PoliceOfficerRepository policeOfficerRepository;
    private final PoliceStationRepository policeStationRepository;
    private final StationAppointmentRepository appointmentRepository;
    private final OfficerShiftRepository officerShiftRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public Response<PoliceOfficer> savePoliceOfficer(PoliceOfficerDto dto) {
        if (dto == null) return Response.error("Police officer DTO cannot be null");

        PoliceOfficer officer = new PoliceOfficer();
        if (dto.getUid() != null) {
            // UPDATE
            Optional<PoliceOfficer> existing = policeOfficerRepository.findByUid(dto.getUid());
            if (existing.isEmpty()) return Response.error("Invalid police officer provided");

            officer = existing.get();
            Utils.copyProperties(dto, officer);
            officer.setCode(dto.getCode());

            PoliceStation updatedStation = null;
            if (dto.getStationUid() != null) {
                Optional<PoliceStation> stationOpt = policeStationRepository.findByUid(dto.getStationUid());
                if (stationOpt.isEmpty()) return Response.error("Police station not found");
                updatedStation = stationOpt.get();
                officer.setPoliceStation(updatedStation);
            }

            if (dto.getUserUid() != null) {
                Optional<User> userOpt = userRepository.findByUid(dto.getUserUid());
                if (userOpt.isEmpty()) return Response.error("User not found");
                User linkedUser = userOpt.get();
                if (updatedStation != null) {
                    linkedUser.setPoliceStation(updatedStation);
                    userRepository.save(linkedUser);
                }
                officer.setUserAccount(linkedUser);
            }

            if (dto.getDepartmentUid() != null) {
                Optional<Department> departmentOpt = departmentRepository.findByUid(dto.getDepartmentUid());
                if (departmentOpt.isEmpty()) return Response.error("Department not found");
                officer.setDepartment(departmentOpt.get());
            }
            officer.update();

        } else {
            // CREATE
            if (dto.getBadgeNumber() == null) return Response.error("Badge number is required");
            if (dto.getCode() == null) return Response.error("Officer rank is required");
            if (dto.getDepartmentUid() == null) return Response.error("Department is required");
            if (dto.getStationUid() == null) return Response.error("Police station is required");

            Optional<PoliceStation> stationOpt = policeStationRepository.findByUid(dto.getStationUid());
            if (stationOpt.isEmpty()) return Response.error("Police station not found");
            PoliceStation station = stationOpt.get();

            Optional<Department> departmentOpt = departmentRepository.findByUid(dto.getDepartmentUid());
            if (departmentOpt.isEmpty()) return Response.error("Department not found");

            User user;
            if (dto.getUserUid() != null) {
                Optional<User> userOpt = userRepository.findByUid(dto.getUserUid());
                if (userOpt.isEmpty()) return Response.error("User not found");
                user = userOpt.get();
                user.setPoliceStation(station);
                user.setRole(Role.POLICE_OFFICER);
                userRepository.save(user);
            } else {
                if (dto.getName() == null || dto.getName().trim().isEmpty())
                    return Response.error("Officer name is required");
                if (dto.getPhoneNumber() == null || dto.getPhoneNumber().trim().isEmpty())
                    return Response.error("Phone number is required");
                if (userRepository.findByPhoneNumber(dto.getPhoneNumber()).isPresent())
                    return Response.error("A user with this phone number already exists");

                User newUser = new User();
                newUser.setName(dto.getName().trim());
                newUser.setPhoneNumber(dto.getPhoneNumber().trim());
                newUser.setRole(Role.POLICE_OFFICER);
                newUser.setVerified(true);
                newUser.setPoliceStation(station);
                user = userRepository.save(newUser);
                log.info("Created user account inline for officer: {}", user.getPhoneNumber());
            }

            boolean alreadyAssigned = policeOfficerRepository.existsByUserAccount(user);
            if (alreadyAssigned) return Response.error("This user is already assigned as a police officer");

            Utils.copyProperties(dto, officer);
            officer.setCode(dto.getCode());
            officer.setUserAccount(user);
            officer.setDepartment(departmentOpt.get());
            officer.setPoliceStation(station);
        }

        try {
            PoliceOfficer saved = policeOfficerRepository.save(officer);
            log.info("Saved police officer with badge: {}", saved.getBadgeNumber());

            if (dto.getUid() == null && dto.getAppointmentPosition() != null) {
                StationAppointment appointment = new StationAppointment();
                appointment.setOfficer(saved);
                appointment.setPoliceStation(saved.getPoliceStation());
                appointment.setPosition(dto.getAppointmentPosition());
                appointment.setStatus(AppointmentStatus.ACTIVE);
                appointment.setStartDate(dto.getAppointmentStartDate() != null
                        ? dto.getAppointmentStartDate() : java.time.LocalDate.now());
                appointment.setEndDate(dto.getAppointmentEndDate());
                appointment.setNotes(dto.getAppointmentNotes());
                appointmentRepository.save(appointment);
                log.info("Appointment created for officer {} as {}", saved.getBadgeNumber(), appointment.getPosition());
            }

            return Response.success(saved);
        } catch (Exception e) {
            log.error("Failed to save police officer: {}", e.getMessage());
            return Response.error("Failed to save police officer: " + Utils.getExceptionMessage(e));
        }
    }

    public Response<PoliceOfficer> getPoliceOfficer(String uid) {
        if (uid == null) return new Response<>("Uid is required");
        return policeOfficerRepository.findByUid(uid)
                .map(Response::new)
                .orElseGet(() -> new Response<>("Invalid police officer provided"));
    }

    public Response<PoliceOfficer> deletePoliceOfficer(String uid) {
        if (uid == null) return new Response<>("Uid is required");
        Optional<PoliceOfficer> oPoliceOfficer = policeOfficerRepository.findByUid(uid);
        if (oPoliceOfficer.isEmpty()) return new Response<>("Invalid police officer provided");
        if (!oPoliceOfficer.get().getIsActive()) return new Response<>("Police officer already deleted");
        oPoliceOfficer.get().delete();
        PoliceOfficer policeOfficer = oPoliceOfficer.get();
        try {
            policeOfficerRepository.save(policeOfficer);
            log.info("Police officer deleted: {}", policeOfficer.getUserAccount().getName());
        } catch (Exception e) {
            log.error("Failed to delete police officer: {}", e.getMessage());
            return new Response<>(Utils.getExceptionMessage(e));
        }
        return Response.success(policeOfficer);
    }

    public ResponsePage<PoliceOfficerResponse> getPoliceOfficers(PageableParam pageableParam) {
        if (LoggedUser.isAgencyAdmin()) {
            String agencyUid = LoggedUser.getAgencyUid();
            if (agencyUid != null) {
                return new ResponsePage<>(policeOfficerRepository.getPoliceOfficersForAgencyAdmin(
                        pageableParam.getPageable(false), pageableParam.getIsActive(),
                        pageableParam.key(), agencyUid).map(this::toResponse));
            }
        }
        String stationUid = LoggedUser.getPoliceStationUid();
        return new ResponsePage<>(policeOfficerRepository.getPoliceOfficers(
                pageableParam.getPageable(false), pageableParam.getIsActive(), pageableParam.key(), stationUid)
                .map(this::toResponse));
    }

    public ResponsePage<PoliceOfficerResponse> getPoliceOfficersByStation(PageableParam pageableParam, String stationUid) {
        return new ResponsePage<>(policeOfficerRepository.getPoliceOfficersByStation(
                pageableParam.getPageable(false), pageableParam.getIsActive(), pageableParam.key(), stationUid)
                .map(this::toResponse));
    }

    @Transactional
    public Response<PoliceOfficerResponse> updateOfficerProfile(String uid, OfficerProfileUpdateDto dto) {
        Optional<PoliceOfficer> opt = policeOfficerRepository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Officer not found");
        PoliceOfficer officer = opt.get();
        User user = officer.getUserAccount();
        if (user == null) return Response.error("Officer has no linked user account");

        if (dto.getName() != null && !dto.getName().isBlank()) {
            user.setName(dto.getName().trim());
        }
        if (dto.getUsername() != null && !dto.getUsername().isBlank()) {
            String newUsername = dto.getUsername().trim();
            if (!newUsername.equals(user.getUsername())) {
                if (userRepository.existsByUsername(newUsername))
                    return Response.error("Username '" + newUsername + "' is already taken");
                user.setUsername(newUsername);
            }
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            if (dto.getPassword().length() < 6)
                return Response.error("Password must be at least 6 characters");
            user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        user.update();
        userRepository.save(user);
        log.info("Profile updated for officer: {}", officer.getBadgeNumber());
        return Response.success(toResponse(officer));
    }

    @Transactional
    public Response<PoliceOfficerResponse> transferOfficer(String uid, String newStationUid) {
        Optional<PoliceOfficer> opt = policeOfficerRepository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Officer not found");
        Optional<PoliceStation> stationOpt = policeStationRepository.findByUid(newStationUid);
        if (stationOpt.isEmpty()) return Response.error("Station not found");

        PoliceOfficer officer = opt.get();
        PoliceStation newStation = stationOpt.get();

        // Mark existing active appointment as TRANSFERRED
        List<StationAppointment> activeAppts = appointmentRepository
                .findByOfficerUidAndStatusAndIsActiveTrue(uid, AppointmentStatus.ACTIVE);
        for (StationAppointment appt : activeAppts) {
            appt.setStatus(AppointmentStatus.TRANSFERRED);
            appt.setEndDate(LocalDate.now());
            appt.update();
            appointmentRepository.save(appt);
        }

        officer.setPoliceStation(newStation);
        officer.update();
        policeOfficerRepository.save(officer);

        if (officer.getUserAccount() != null) {
            officer.getUserAccount().setPoliceStation(newStation);
            userRepository.save(officer.getUserAccount());
        }

        log.info("Officer {} transferred to station {}", officer.getBadgeNumber(), newStation.getName());
        return Response.success(toResponse(officer));
    }

    private PoliceOfficerResponse toResponse(PoliceOfficer o) {
        PoliceOfficerResponse r = new PoliceOfficerResponse();
        r.setUid(o.getUid());
        r.setBadgeNumber(o.getBadgeNumber());
        r.setCode(o.getCode());
        if (o.getUserAccount() != null) {
            r.setName(o.getUserAccount().getName());
            r.setPhoneNumber(o.getUserAccount().getPhoneNumber());
        }
        if (o.getPoliceStation() != null) {
            r.setStationUid(o.getPoliceStation().getUid());
            r.setStationName(o.getPoliceStation().getName());
        }
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        r.setOnDuty(officerShiftRepository
                .findByOfficerUidAndShiftDateAndStartTimeBeforeAndEndTimeAfter(o.getUid(), today, now, now)
                .isPresent());
        List<StationAppointment> appts = appointmentRepository
                .findByOfficerUidAndStatusAndIsActiveTrue(o.getUid(), AppointmentStatus.ACTIVE);
        if (!appts.isEmpty()) {
            StationAppointment appt = appts.get(0);
            r.setAppointmentPosition(appt.getPosition());
            r.setAppointmentStatus(appt.getStatus());
            r.setAppointmentStartDate(appt.getStartDate());
        }
        return r;
    }
}
