package com.smartincident.incidentbackend.fire.service;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.emergency.repository.EmergencyUnitRepository;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.fire.dto.FireOfficerDto;
import com.smartincident.incidentbackend.fire.entity.FireOfficer;
import com.smartincident.incidentbackend.fire.repository.FireOfficerRepository;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FireOfficerService {

    private final FireOfficerRepository officerRepository;
    private final EmergencyUnitRepository unitRepository;
    private final UserRepository userRepository;

    // ── Save (create / update) ─────────────────────────────────────────────

    @Transactional
    public Response<FireOfficer> saveOfficer(FireOfficerDto dto) {
        if (dto == null) return Response.error("Officer data is required");

        FireOfficer officer;

        if (dto.getUid() != null) {
            // UPDATE
            Optional<FireOfficer> existing = officerRepository.findByUid(dto.getUid());
            if (existing.isEmpty()) return Response.error("Fire officer not found");
            officer = existing.get();

            if (dto.getRank() != null) officer.setRank(dto.getRank());
            if (dto.getIsOnDuty() != null) officer.setIsOnDuty(dto.getIsOnDuty());

            if (dto.getEmergencyUnitUid() != null) {
                Optional<EmergencyUnit> unitOpt = unitRepository.findByUid(dto.getEmergencyUnitUid());
                if (unitOpt.isEmpty()) return Response.error("Emergency unit not found");
                officer.setEmergencyUnit(unitOpt.get());
                // keep user.emergencyUnit in sync
                if (officer.getUserAccount() != null) {
                    officer.getUserAccount().setEmergencyUnit(unitOpt.get());
                    userRepository.save(officer.getUserAccount());
                }
            }
            officer.update();

        } else {
            // CREATE
            if (dto.getServiceNumber() == null) return Response.error("Service number is required");
            if (dto.getRank() == null) return Response.error("Rank is required");
            if (dto.getEmergencyUnitUid() == null) return Response.error("Emergency unit is required");

            Optional<EmergencyUnit> unitOpt = unitRepository.findByUid(dto.getEmergencyUnitUid());
            if (unitOpt.isEmpty()) return Response.error("Emergency unit not found");
            EmergencyUnit unit = unitOpt.get();

            User user;
            if (dto.getUserUid() != null) {
                Optional<User> userOpt = userRepository.findByUid(dto.getUserUid());
                if (userOpt.isEmpty()) return Response.error("User not found");
                user = userOpt.get();
                user.setEmergencyUnit(unit);
                user.setRole(Role.FIRE_OFFICER);
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
                newUser.setRole(Role.FIRE_OFFICER);
                newUser.setVerified(true);
                newUser.setEmergencyUnit(unit);
                user = userRepository.save(newUser);
                log.info("Created user account for fire officer: {}", user.getPhoneNumber());
            }

            if (officerRepository.existsByUserAccountUid(user.getUid()))
                return Response.error("This user is already registered as a fire officer");

            officer = FireOfficer.builder()
                    .serviceNumber(dto.getServiceNumber())
                    .rank(dto.getRank())
                    .emergencyUnit(unit)
                    .userAccount(user)
                    .isOnDuty(Boolean.TRUE.equals(dto.getIsOnDuty()))
                    .build();
        }

        try {
            FireOfficer saved = officerRepository.save(officer);
            log.info("Fire officer saved: {}", saved.getServiceNumber());
            return Response.success(saved);
        } catch (Exception e) {
            log.error("Failed to save fire officer: {}", e.getMessage());
            return Response.error("Failed to save fire officer: " + Utils.getExceptionMessage(e));
        }
    }

    // ── Read ───────────────────────────────────────────────────────────────

    public Response<FireOfficer> getOfficer(String uid) {
        if (uid == null) return Response.error("UID is required");
        return officerRepository.findByUid(uid)
                .map(Response::success)
                .orElseGet(() -> Response.error("Fire officer not found"));
    }

    public ResponsePage<FireOfficer> getOfficers(PageableParam param, String unitUid) {
        String resolvedUnitUid = unitUid != null ? unitUid : LoggedUser.getEmergencyUnitUid();
        return new ResponsePage<>(officerRepository.search(
                param.getPageable(false), param.getIsActive(), param.key(), resolvedUnitUid));
    }

    // ── Delete ─────────────────────────────────────────────────────────────

    @Transactional
    public Response<FireOfficer> deleteOfficer(String uid) {
        if (uid == null) return Response.error("UID is required");
        Optional<FireOfficer> opt = officerRepository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Fire officer not found");
        FireOfficer officer = opt.get();
        if (!officer.getIsActive()) return Response.error("Officer already deleted");
        officer.delete();
        try {
            officerRepository.save(officer);
            log.info("Fire officer deleted: {}", uid);
            return Response.success(officer);
        } catch (Exception e) {
            return Response.error("Failed to delete officer: " + Utils.getExceptionMessage(e));
        }
    }

    // ── Duty toggle ────────────────────────────────────────────────────────

    @Transactional
    public Response<FireOfficer> toggleDuty(String uid, boolean onDuty) {
        Optional<FireOfficer> opt = officerRepository.findByUid(uid);
        if (opt.isEmpty()) return Response.error("Fire officer not found");
        FireOfficer officer = opt.get();
        officer.setIsOnDuty(onDuty);
        officer.update();
        return Response.success(officerRepository.save(officer));
    }
}
