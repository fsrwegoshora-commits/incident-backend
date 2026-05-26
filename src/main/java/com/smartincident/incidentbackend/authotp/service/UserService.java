package com.smartincident.incidentbackend.authotp.service;

import com.smartincident.incidentbackend.authotp.dto.UserDto;
import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.security.JwtAuthInterceptor;
import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.emergency.repository.EmergencyUnitRepository;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.medical.entity.Hospital;
import com.smartincident.incidentbackend.medical.repository.HospitalRepository;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
import com.smartincident.incidentbackend.police.repository.PoliceStationRepository;
import com.smartincident.incidentbackend.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final EmergencyUnitRepository emergencyUnitRepository;
    private final HospitalRepository hospitalRepository;
    private final PoliceStationRepository policeStationRepository;
    private final OtpService otpService;
    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final BCryptPasswordEncoder passwordEncoder;

    public Response<User> userRegistration(UserDto userDto) {
        if (userDto == null)
            return Response.error("User is required");

        User user = new User();
        if (userDto.getUid() != null) {
            Optional<User> oUser = userRepository.findByUid(userDto.getUid());
            if (!oUser.isPresent())
                return Response.error("Invalid user provided");
            user = oUser.get();
            user.update();
        } else {
            if (userDto.getName() == null)
                return Response.error("Username is required");
            if (userDto.getPhoneNumber() == null)
                return Response.error("Phone number is required");

            Optional<User> existingUser = userRepository.findByPhoneNumber(userDto.getPhoneNumber());

            if (existingUser.isPresent()) {
                User existing = existingUser.get();

                if (!existing.getIsActive() || existing.getIsDeleted()) {
                    log.info("Reactivating previously deleted user: {}", userDto.getPhoneNumber());

                    existing.setIsActive(true);
                    existing.setIsDeleted(false);
                    existing.setName(userDto.getName());
                    existing.setUpdatedAt(LocalDateTime.now());
                    user = existing;

                    tokenBlacklistService.removeUserFromBlacklist(userDto.getPhoneNumber());
                    log.info("Removed user from blacklist for re-registration: {}", userDto.getPhoneNumber());

                } else {
                    return Response.error("Phone number already registered");
                }
            } else {
                user.setName(userDto.getName());
                user.setPhoneNumber(userDto.getPhoneNumber());
                user.setVerified(false);
                user.setRole(Role.CITIZEN);
            }
        }

        Utils.copyProperties(userDto, user);
        try {
            user = userRepository.save(user);
            log.info("User saved successfully: {}", user.getPhoneNumber());

            if (userDto.getOtpCode() != null) {
                Response<Boolean> otpResponse = otpService.verifyOtp(user.getPhoneNumber(), userDto.getOtpCode());
                if (otpResponse.success() && otpResponse.getData()) {
                    user.setVerified(true);
                    user = userRepository.save(user);
                    log.info("User verified with OTP: {}", user.getPhoneNumber());
                } else {
                    return Response.error("OTP verification failed: " + otpResponse.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to save user: {}", e.getMessage());
            return Response.error("Failed to save user: " + Utils.getExceptionMessage(e));
        }
        return new Response<>(user);
    }

        public Response<User> getUser(String uid) {
            if (uid == null)
                return new Response<>("Id is required");
            Optional<User> oUser = userRepository.findByUid(uid);
            if (oUser.isPresent())
                return new Response<>(oUser.get());
            return new Response<>("Invalid id provided");
        }

        public Response<User> deleteUser(String uid) {
            if (uid == null)
                return new Response<>("Id is required");
            Optional<User> oUser = userRepository.findByUid(uid);
            if (!oUser.isPresent())
                return new Response<>("Invalid id provided");
            if (!oUser.get().getIsActive())
                return new Response<>("User already deleted");
            oUser.get().delete();
            User user = oUser.get();
            try {
                userRepository.save(user);
                log.info("User deleted successfully: {}", user.getPhoneNumber());
            } catch (Exception e) {
                log.error("Failed to delete user: {}", e.getMessage());
                String message = Utils.getExceptionMessage(e);
                return new Response<>(message);
            }
            return Response.success(user);
        }

        public ResponsePage<User> getUsers(PageableParam pageableParam) {
            if (LoggedUser.isRoot()) {
                return new ResponsePage<>(userRepository.findByKey(
                        pageableParam.getPageable(true), pageableParam.getIsActive(), pageableParam.key(), null));
            }
            String agencyUid = LoggedUser.getAgencyUid();
            if (LoggedUser.isAgencyAdmin() && agencyUid != null) {
                return new ResponsePage<>(userRepository.findByKeyAndAgency(
                        pageableParam.getPageable(true), pageableParam.getIsActive(), pageableParam.key(), agencyUid));
            }
            String stationUid = LoggedUser.getAnyStationUid();
            return new ResponsePage<>(userRepository.findByKey(
                    pageableParam.getPageable(true), pageableParam.getIsActive(), pageableParam.key(), stationUid));
        }

        public Response<User> getUserByPhone(String phone) {
            return new Response<>(userRepository.findByPhoneNumber(phone).orElse(null));
        }

        public Response<User> registerSpecialUser(UserDto userDto) {
            if (userDto == null) return Response.error("User DTO cannot be null");

            // AGENCY_ADMIN can only create users within their own agency
            User requester = getUserByPhone(
                    jwtAuthInterceptor.extractPhoneFromRequest()).getData();
            if (requester != null && requester.getRole() == Role.AGENCY_ADMIN) {
                if (requester.getAgency() == null)
                    return Response.error("Agency admin has no agency assigned");
                if (userDto.getUid() != null) {
                    Optional<User> existingOpt = userRepository.findByUid(userDto.getUid());
                    if (existingOpt.isPresent()) {
                        User existing = existingOpt.get();
                        String targetAgency = existing.getAgency() != null ? existing.getAgency().getUid() : null;
                        if (!requester.getAgency().getUid().equals(targetAgency)) {
                            return Response.error("Access denied: cannot manage users from another agency");
                        }
                    }
                }
            }
            if (requester != null && requester.getRole() == Role.DISPATCH_CENTER_ADMIN) {
                if (requester.getEmergencyUnit() == null)
                    return Response.error("Dispatch center admin has no dispatch center assigned");
                Role targetRole = userDto.getRole();
                if (targetRole != Role.DISPATCHER && targetRole != Role.DISPATCHER_SUPERVISOR)
                    return Response.error("Dispatch center admins can only create DISPATCHER or DISPATCHER_SUPERVISOR users");
                userDto.setEmergencyUnitUid(requester.getEmergencyUnit().getUid());
            }

            User user = new User();
            boolean isUpdate = userDto.getUid() != null;

            if (isUpdate) {
                Optional<User> oUser = userRepository.findByUid(userDto.getUid());
                if (!oUser.isPresent()) return Response.error("Invalid user provided");
                user = oUser.get();
                Utils.copyProperties(userDto, user);
                user.setRole(userDto.getRole());
                // Update username if provided and not already taken by another user
                if (userDto.getUsername() != null && !userDto.getUsername().isBlank()) {
                    String newUsername = userDto.getUsername().trim();
                    if (!newUsername.equals(user.getUsername())) {
                        if (userRepository.existsByUsername(newUsername))
                            return Response.error("Username '" + newUsername + "' is already taken");
                        user.setUsername(newUsername);
                    }
                }
                // Update password if provided
                if (userDto.getPassword() != null && !userDto.getPassword().isBlank()) {
                    user.setPasswordHash(passwordEncoder.encode(userDto.getPassword()));
                }
                user.update();
            } else {
                if (userDto.getRole() == Role.CITIZEN)
                    return Response.error("Use normal registration for citizens");
                if (userDto.getPhoneNumber() == null || userDto.getPhoneNumber().trim().isEmpty())
                    return Response.error("Phone number is required");
                // Username is required for management users
                if (userDto.getUsername() == null || userDto.getUsername().isBlank())
                    return Response.error("Username is required for management users");
                if (userDto.getPassword() == null || userDto.getPassword().isBlank())
                    return Response.error("Password is required for management users");
                String username = userDto.getUsername().trim();
                if (userRepository.existsByUsername(username))
                    return Response.error("Username '" + username + "' is already taken");
                Utils.copyProperties(userDto, user);
                user.setUsername(username);
                user.setPasswordHash(passwordEncoder.encode(userDto.getPassword()));
                user.setRole(userDto.getRole());
                user.setVerified(true);
                // Inherit agency from the creating AGENCY_ADMIN
                if (requester != null && requester.getRole() == Role.AGENCY_ADMIN && requester.getAgency() != null) {
                    user.setAgency(requester.getAgency());
                }
            }

            Response<Void> assignResult = assignUnit(user, userDto, isUpdate);
            if (assignResult != null) return Response.error(assignResult.getMessage());

            try {
                User savedUser = userRepository.save(user);
                log.info("Registered special user: {} ({})", savedUser.getPhoneNumber(), savedUser.getRole());
                return Response.success(savedUser);
            } catch (Exception e) {
                log.error("Failed to register special user: {}", e.getMessage());
                return Response.error("Failed to register user: " + e.getMessage());
            }
        }

        /**
         * Assigns the correct station/unit to the user based on their role.
         * Returns a non-null error Response if assignment fails; null on success.
         */
        private Response<Void> assignUnit(User user, UserDto dto, boolean isUpdate) {
            Role role = user.getRole();
            if (role == null) return null;

            switch (role) {
                case POLICE_OFFICER -> {
                    if (dto.getStationUid() != null) {
                        PoliceStation station = policeStationRepository.findByUid(dto.getStationUid()).orElse(null);
                        if (station == null) return Response.error("Police station not found");
                        user.setPoliceStation(station);
                        user.setEmergencyUnit(null);
                    } else if (!isUpdate) {
                        return Response.error("Police station is required for POLICE_OFFICER");
                    }
                }
                case STATION_ADMIN -> {
                    // STATION_ADMIN can run a police station, a fire/medical emergency unit, or a hospital ambulance unit
                    if (dto.getEmergencyUnitUid() != null) {
                        EmergencyUnit unit = emergencyUnitRepository.findByUid(dto.getEmergencyUnitUid()).orElse(null);
                        if (unit == null) return Response.error("Emergency unit not found");
                        user.setEmergencyUnit(unit);
                        user.setPoliceStation(null);
                    } else if (dto.getHospitalUid() != null) {
                        Hospital hospital = hospitalRepository.findByUid(dto.getHospitalUid()).orElse(null);
                        if (hospital == null) return Response.error("Hospital not found");
                        if (hospital.getAmbulanceUnit() == null)
                            return Response.error("Hospital '" + hospital.getName() + "' has no ambulance unit configured");
                        user.setEmergencyUnit(hospital.getAmbulanceUnit());
                        user.setPoliceStation(null);
                    } else if (dto.getStationUid() != null) {
                        PoliceStation station = policeStationRepository.findByUid(dto.getStationUid()).orElse(null);
                        if (station == null) return Response.error("Police station not found");
                        user.setPoliceStation(station);
                        user.setEmergencyUnit(null);
                    } else if (!isUpdate) {
                        return Response.error("A policeStationUid, emergencyUnitUid, or hospitalUid is required for STATION_ADMIN");
                    }
                }
                case FIRE_OFFICER -> {
                    // Fire users belong to an EmergencyUnit (fire type)
                    if (dto.getEmergencyUnitUid() != null) {
                        EmergencyUnit unit = emergencyUnitRepository.findByUid(dto.getEmergencyUnitUid()).orElse(null);
                        if (unit == null) return Response.error("Fire unit not found");
                        user.setEmergencyUnit(unit);
                        user.setPoliceStation(null);
                    } else if (!isUpdate) {
                        return Response.error("Fire unit is required for role " + role);
                    }
                }
                case MEDIC -> {
                    // Medical users are linked through their hospital's ambulance unit
                    if (dto.getHospitalUid() != null) {
                        Hospital hospital = hospitalRepository.findByUid(dto.getHospitalUid()).orElse(null);
                        if (hospital == null) return Response.error("Hospital not found");
                        if (hospital.getAmbulanceUnit() == null)
                            return Response.error("Hospital '" + hospital.getName() + "' has no ambulance unit configured");
                        user.setEmergencyUnit(hospital.getAmbulanceUnit());
                        user.setPoliceStation(null);
                    } else if (!isUpdate) {
                        return Response.error("Hospital is required for role " + role);
                    }
                }
                case DISPATCH_CENTER_ADMIN, DISPATCHER_SUPERVISOR, DISPATCHER -> {
                    // All dispatch roles must belong to a Dispatch Center
                    if (dto.getEmergencyUnitUid() != null) {
                        EmergencyUnit unit = emergencyUnitRepository.findByUid(dto.getEmergencyUnitUid()).orElse(null);
                        if (unit == null) return Response.error("Dispatch center not found");
                        user.setEmergencyUnit(unit);
                        user.setPoliceStation(null);
                    } else if (!isUpdate) {
                        return Response.error("A Dispatch Center (emergencyUnitUid) is required for " + role + " role");
                    }
                    if (dto.getAppointment() != null) {
                        user.setAppointment(dto.getAppointment());
                    }
                }
                default -> {
                    // ROOT, AGENCY_ADMIN, CITIZEN — no unit required
                }
            }
            return null; // success
        }

        public ResponsePage<User> getUsersByStation(PageableParam pageableParam, String stationUid) {
            return new ResponsePage<>(userRepository.getUsersByStation(pageableParam.getPageable(true), pageableParam.getIsActive(), pageableParam.key(), stationUid));
        }

    public Response<User> changeUserRole(String uid, Role newRole, String stationUid) {
        if (uid == null)
            return Response.error("User id is required");
        if (newRole == null)
            return Response.error("Role is required");

        Optional<User> oUser = userRepository.findByUid(uid);
        if (oUser.isEmpty())
            return Response.error("User not found");

        User user = oUser.get();

        // AGENCY_ADMIN can only change roles of users within their agency
        User requester = getUserByPhone(jwtAuthInterceptor.extractPhoneFromRequest()).getData();
        if (requester != null && requester.getRole() == Role.AGENCY_ADMIN) {
            String requesterAgency = requester.getAgency() != null ? requester.getAgency().getUid() : null;
            String targetAgency   = user.getAgency()    != null ? user.getAgency().getUid()    : null;
            if (requesterAgency == null || !requesterAgency.equals(targetAgency)) {
                return Response.error("Access denied: cannot change role of a user from another agency");
            }
            // AGENCY_ADMIN cannot escalate to ROOT
            if (newRole == Role.ROOT) {
                return Response.error("Agency admins cannot assign ROOT role");
            }
        }

        // CITIZEN and ROOT need no unit; dispatch/agency roles may optionally have one
        boolean unitExempt = newRole == Role.CITIZEN || newRole == Role.ROOT;
        boolean unitOptional = newRole == Role.DISPATCHER || newRole == Role.DISPATCHER_SUPERVISOR
                || newRole == Role.DISPATCH_CENTER_ADMIN || newRole == Role.AGENCY_ADMIN;

        if (!unitExempt && !unitOptional && stationUid == null) {
            return Response.error("Emergency unit is required for role " + newRole);
        }
        if (stationUid != null) {
            Optional<EmergencyUnit> unitOpt = emergencyUnitRepository.findByUid(stationUid);
            if (unitOpt.isEmpty()) return Response.error("Emergency unit not found");
            user.setEmergencyUnit(unitOpt.get());
        } else if (unitExempt || unitOptional) {
            user.setEmergencyUnit(null);
        }

        user.setRole(newRole);
        user.update();

        try {
            userRepository.save(user);
            jwtService.invalidateAllUserTokens(user.getPhoneNumber());
            log.info("Role changed to {} for user: {}", newRole, user.getPhoneNumber());
        } catch (Exception e) {
            log.error("Failed to change role: {}", e.getMessage());
            return Response.error("Failed to change role: " + Utils.getExceptionMessage(e));
        }

        return Response.success(user);
    }

    public Response<User> deleteOwnAccount(String phoneNumber, String currentToken) { // ADD currentToken PARAMETER
        Optional<User> oUser = userRepository.findByPhoneNumber(phoneNumber);
        if (!oUser.isPresent())
            return new Response<>("User not found");

        User user = oUser.get();
        if (user.getRole() != Role.CITIZEN) {
            return new Response<>("Only citizens can delete their own accounts");
        }

        if (!user.getIsActive())
            return new Response<>("Account already deleted");

        try {
            userRepository.delete(user);
            jwtService.invalidateToken(currentToken);
            jwtService.invalidateAllUserTokens(phoneNumber);

            log.info("User self-deleted account and invalidated tokens: {}", user.getPhoneNumber());

        } catch (Exception e) {
            log.error("Failed to delete account: {}", e.getMessage());
            String message = Utils.getExceptionMessage(e);
            return new Response<>(message);
        }

        return Response.success(user);
    }
}