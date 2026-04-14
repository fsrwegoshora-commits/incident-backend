package com.smartincident.incidentbackend.police.service;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.enums.AppointmentPosition;
import com.smartincident.incidentbackend.enums.AppointmentStatus;
import com.smartincident.incidentbackend.enums.Role;
import jakarta.annotation.PostConstruct;
import com.smartincident.incidentbackend.police.dto.AppointmentDto;
import com.smartincident.incidentbackend.police.dto.PoliceOfficerDto;
import com.smartincident.incidentbackend.setting.entity.Department;
import com.smartincident.incidentbackend.police.entity.PoliceOfficer;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
import com.smartincident.incidentbackend.police.entity.StationAppointment;
import com.smartincident.incidentbackend.setting.repository.DepartmentRepository;
import com.smartincident.incidentbackend.police.repository.PoliceOfficerRepository;
import com.smartincident.incidentbackend.police.repository.PoliceStationRepository;
import com.smartincident.incidentbackend.police.repository.StationAppointmentRepository;
import com.smartincident.incidentbackend.utils.*;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
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
@GraphQLApi
public class PoliceOfficerService {
    private final DepartmentRepository departmentRepository;
    private final PoliceOfficerRepository policeOfficerRepository;
    private final PoliceStationRepository policeStationRepository;
    private final StationAppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    /** One-time fix: sync user.station from the PoliceOfficer record for any existing officers missing it. */
    @PostConstruct
    @Transactional
    public void backfillUserStations() {
        try {
            List<PoliceOfficer> officers = policeOfficerRepository.findAll();
            int fixed = 0;
            for (PoliceOfficer officer : officers) {
                User u = officer.getUserAccount();
                if (u != null && officer.getStation() != null && u.getStation() == null) {
                    u.setStation(officer.getStation());
                    userRepository.save(u);
                    fixed++;
                }
            }
            if (fixed > 0) log.info("Backfilled station on {} user accounts from PoliceOfficer records", fixed);
        } catch (Exception e) {
            log.warn("Station backfill skipped: {}", e.getMessage());
        }
    }

    @Transactional
    public Response<PoliceOfficer> savePoliceOfficer(PoliceOfficerDto dto) {
        if (dto == null) return Response.error("Police officer DTO cannot be null");

        PoliceOfficer officer = new PoliceOfficer();
        if (dto.getUid() != null) {
            Optional<PoliceOfficer> existing = policeOfficerRepository.findByUid(dto.getUid());
            if (existing.isEmpty()) return Response.error("Invalid police officer provided");

            officer = existing.get();
            Utils.copyProperties(dto, officer);
            officer.setCode(dto.getCode());

            PoliceStation updatedStation = null;
            if (dto.getStationUid() != null) {
                Optional<PoliceStation> stationOpt = policeStationRepository.findByUid(dto.getStationUid());
                if (stationOpt.isEmpty()) return Response.error("Station not found");
                updatedStation = stationOpt.get();
                officer.setStation(updatedStation);
            }

            if (dto.getUserUid() != null) {
                Optional<User> userOpt = userRepository.findByUid(dto.getUserUid());
                if (userOpt.isEmpty()) return Response.error("User not found");
                User linkedUser = userOpt.get();
                // Sync station on the user account when station changes
                if (updatedStation != null) {
                    linkedUser.setStation(updatedStation);
                    userRepository.save(linkedUser);
                }
                officer.setUserAccount(linkedUser);
            }

            if(dto.getDepartmentUid() != null){
                Optional<Department> departmentOpt = departmentRepository.findByUid(dto.getDepartmentUid());
                if(departmentOpt.isEmpty()) return Response.error("department not found");
                officer.setDepartment(departmentOpt.get());
            }
            officer.update();
        }

        // CREATE FLOW
        else {
            if (dto.getBadgeNumber() == null) return Response.error("Badge number is required");
            if (dto.getCode() == null) return Response.error("Officer rank is required");
            if (dto.getDepartmentUid() == null) return Response.error("Department is required");
            if (dto.getStationUid() == null) return Response.error("Station is required");

            Optional<PoliceStation> stationOpt = policeStationRepository.findByUid(dto.getStationUid());
            if (stationOpt.isEmpty()) return Response.error("Station not found");

            Optional<Department> departmentOpt = departmentRepository.findByUid(dto.getDepartmentUid());
            if (departmentOpt.isEmpty()) return Response.error("Department not found");

            User user;
            if (dto.getUserUid() != null) {
                // Link to existing user account
                Optional<User> userOpt = userRepository.findByUid(dto.getUserUid());
                if (userOpt.isEmpty()) return Response.error("User not found");
                user = userOpt.get();
                // Sync station on the user record so it appears in user management
                user.setStation(stationOpt.get());
                user.setRole(Role.POLICE_OFFICER);
                userRepository.save(user);
            } else {
                // Create user account inline
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
                newUser.setStation(stationOpt.get());
                user = userRepository.save(newUser);
                log.info("Created user account inline for officer: {}", user.getPhoneNumber());
            }

            boolean alreadyAssigned = policeOfficerRepository.existsByUserAccount(user);
            if (alreadyAssigned) return Response.error("This user is already assigned as a police officer");

            Utils.copyProperties(dto, officer);
            officer.setCode(dto.getCode());
            officer.setUserAccount(user);
            officer.setDepartment(departmentOpt.get());
            officer.setStation(stationOpt.get());
        }

        try {
            PoliceOfficer saved = policeOfficerRepository.save(officer);
            log.info("Successfully saved police officer with badge: {}", saved.getBadgeNumber());

            // Create appointment for new officers when position is provided
            if (dto.getUid() == null && dto.getAppointmentPosition() != null) {
                StationAppointment appointment = new StationAppointment();
                appointment.setOfficer(saved);
                appointment.setStation(saved.getStation());
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
        if (uid == null)
            return new Response<>("Uid is required");
        Optional<PoliceOfficer> optionalPoliceOfficer = policeOfficerRepository.findByUid(uid);
        return optionalPoliceOfficer.map(Response::new).orElseGet(() -> new Response<>("Invalid police police officer provided"));
    }

    public Response<PoliceOfficer> deletePoliceOfficer(String uid) {
        if (uid == null)
            return new Response<>("Uid is required");
        Optional<PoliceOfficer> oPoliceOfficer = policeOfficerRepository.findByUid(uid);
        if (oPoliceOfficer.isEmpty())
            return new Response<>("Invalid police officer provided");
        if (!oPoliceOfficer.get().getIsActive())
            return new Response<>("police officer already deleted");
        oPoliceOfficer.get().delete();
        PoliceOfficer policeOfficer = oPoliceOfficer.get();
        try {
            policeOfficerRepository.save(policeOfficer);
            log.info("police officer deleted successfully: {}", policeOfficer.getUserAccount().getName());
        } catch (Exception e) {
            log.error("Failed to delete police officer: {}", e.getMessage());
            String message = Utils.getExceptionMessage(e);
            return new Response<>(message);
        }
        return Response.success(policeOfficer);
    }

    public ResponsePage<PoliceOfficer> getPoliceOfficers(PageableParam pageableParam) {
        String stationUid = LoggedUser.getStationUid();
        return new ResponsePage<>(policeOfficerRepository.getPoliceOfficers(pageableParam.getPageable(false), pageableParam.getIsActive(), pageableParam.key(), stationUid));
    }

    public ResponsePage<PoliceOfficer> getPoliceOfficersByStation(PageableParam pageableParam, String stationUid) {
        return new ResponsePage<>(policeOfficerRepository.getPoliceOfficersByStation(pageableParam.getPageable(false), pageableParam.getIsActive(), pageableParam.key(), stationUid));
    }
}
