package com.smartincident.incidentbackend.medical.service;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.emergency.repository.EmergencyUnitRepository;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.medical.dto.MedicDto;
import com.smartincident.incidentbackend.medical.entity.Hospital;
import com.smartincident.incidentbackend.medical.entity.Medic;
import com.smartincident.incidentbackend.medical.repository.HospitalRepository;
import com.smartincident.incidentbackend.medical.repository.MedicRepository;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicService {

    private final MedicRepository medicRepository;
    private final HospitalRepository hospitalRepository;
    private final EmergencyUnitRepository unitRepository;
    private final UserRepository userRepository;

    // ── Save ───────────────────────────────────────────────────────────────

    @Transactional
    public Response<Medic> saveMedic(MedicDto dto) {
        if (dto == null) return Response.error("Medic data is required");

        Medic medic;

        if (dto.getUid() != null) {
            // UPDATE
            Optional<Medic> existing = medicRepository.findByUid(dto.getUid());
            if (existing.isEmpty()) return Response.error("Medic not found");
            medic = existing.get();

            if (dto.getRole() != null) medic.setRole(dto.getRole());
            if (dto.getIsOnDuty() != null) medic.setIsOnDuty(dto.getIsOnDuty());

            if (dto.getEmergencyUnitUid() != null) {
                Optional<EmergencyUnit> unitOpt = unitRepository.findByUid(dto.getEmergencyUnitUid());
                if (unitOpt.isEmpty()) return Response.error("Emergency unit not found");
                medic.setEmergencyUnit(unitOpt.get());
                if (medic.getUserAccount() != null) {
                    medic.getUserAccount().setEmergencyUnit(unitOpt.get());
                    userRepository.save(medic.getUserAccount());
                }
            }

            if (dto.getHospitalUid() != null) {
                Optional<Hospital> hospOpt = hospitalRepository.findByUid(dto.getHospitalUid());
                if (hospOpt.isEmpty()) return Response.error("Hospital not found");
                medic.setHospital(hospOpt.get());
            }
            medic.update();

        } else {
            // CREATE
            if (dto.getStaffNumber() == null) return Response.error("Staff number is required");
            if (dto.getRole() == null) return Response.error("Medic role is required");
            if (dto.getHospitalUid() == null) return Response.error("Hospital is required");
            //if (dto.getEmergencyUnitUid() == null) return Response.error("Emergency unit is required");

            Optional<Hospital> hospOpt = hospitalRepository.findByUid(dto.getHospitalUid());
            if (hospOpt.isEmpty()) return Response.error("Hospital not found");
            Hospital hospital = hospOpt.get();

            User user;
            if (dto.getUserUid() != null) {
                Optional<User> userOpt = userRepository.findByUid(dto.getUserUid());
                if (userOpt.isEmpty()) return Response.error("User not found");
                user = userOpt.get();
                user.setEmergencyUnit(hospital.getAmbulanceUnit());
                user.setRole(Role.MEDIC);
                userRepository.save(user);
            } else {
                if (dto.getName() == null || dto.getName().trim().isEmpty())
                    return Response.error("Medic name is required");
                if (dto.getPhoneNumber() == null || dto.getPhoneNumber().trim().isEmpty())
                    return Response.error("Phone number is required");
                if (userRepository.findByPhoneNumber(dto.getPhoneNumber()).isPresent())
                    return Response.error("A user with this phone number already exists");

                User newUser = new User();
                newUser.setName(dto.getName().trim());
                newUser.setPhoneNumber(dto.getPhoneNumber().trim());
                newUser.setRole(Role.MEDIC);
                newUser.setVerified(true);
                newUser.setEmergencyUnit(hospital.getAmbulanceUnit());
                user = userRepository.save(newUser);
                log.info("Created user account for medic: {}", user.getPhoneNumber());
            }

            if (medicRepository.existsByUserAccountUid(user.getUid()))
                return Response.error("This user is already registered as a medic");

            medic = Medic.builder()
                    .staffNumber(dto.getStaffNumber())
                    .role(dto.getRole())
                    .hospital(hospital)
                    .emergencyUnit(hospital.getAmbulanceUnit())
                    .userAccount(user)
                    .isOnDuty(Boolean.TRUE.equals(dto.getIsOnDuty()))
                    .build();
        }

        try {
            Medic saved = medicRepository.save(medic);
            log.info("Medic saved: {}", saved.getStaffNumber());
            return Response.success(saved);
        } catch (Exception e) {
            log.error("Failed to save medic: {}", e.getMessage());
            return Response.error("Failed to save medic: " + Utils.getExceptionMessage(e));
        }
    }

    // ── Read ───────────────────────────────────────────────────────────────

    public Response<Medic> getMedic(String uid) {
        if (uid == null) return Response.error("UID is required");
        return medicRepository.findByUid(uid)
                .map(Response::success)
                .orElseGet(() -> Response.error("Medic not found"));
    }

    public ResponsePage<Medic> getMedics(PageableParam param, String hospitalUid) {
        String resolvedHospitalUid = hospitalUid != null ? hospitalUid : null;
        return new ResponsePage<>(medicRepository.search(
                param.getPageable(false), param.getIsActive(), param.key(), resolvedHospitalUid));
    }

    // ── Delete ─────────────────────────────────────────────────────────────

    @Transactional
    public Response<Medic> deleteMedic(String uid) {
        if (uid == null) return Response.error("UID is required");
        Optional<Medic> opt = medicRepository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Medic not found");
        Medic medic = opt.get();
        if (!medic.getIsActive()) return Response.error("Medic already deleted");
        medic.delete();
        try {
            medicRepository.save(medic);
            log.info("Medic deleted: {}", uid);
            return Response.success(medic);
        } catch (Exception e) {
            return Response.error("Failed to delete medic: " + Utils.getExceptionMessage(e));
        }
    }

    // ── Duty toggle ────────────────────────────────────────────────────────

    @Transactional
    public Response<Medic> toggleDuty(String uid, boolean onDuty) {
        Optional<Medic> opt = medicRepository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Medic not found");
        Medic medic = opt.get();
        medic.setIsOnDuty(onDuty);
        medic.update();
        return Response.success(medicRepository.save(medic));
    }
}
