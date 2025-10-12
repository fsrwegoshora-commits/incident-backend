package com.smartincident.incidentbackend.authotp.service;

import com.smartincident.incidentbackend.authotp.dto.UserDto;
import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.security.JwtAuthInterceptor;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
import com.smartincident.incidentbackend.police.repository.PoliceStationRepository;
import com.smartincident.incidentbackend.utils.*;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@GraphQLApi
public class UserService {
    private final UserRepository userRepository;
    private final PoliceStationRepository policeStationRepository;
    private final OtpService otpService;
    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

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
            String stationUid = LoggedUser.getStationUid();
            return new ResponsePage<>(userRepository.findByKey(pageableParam.getPageable(true), pageableParam.getIsActive(), pageableParam.key(),stationUid));
        }

        public Response<User> getUserByPhone(String phone) {
            return new Response<>(userRepository.findByPhoneNumber(phone).orElse(null));
        }

        public Response<User> registerSpecialUser(UserDto userDto) {
            if (userDto == null) {
                return Response.error("User DTO cannot be null");
            }

            User user = new User();
            if (userDto.getUid() != null) {
                Optional<User> oUser = userRepository.findByUid(userDto.getUid());
                if (!oUser.isPresent())
                    return Response.error("Invalid user provided");
                user = oUser.get();
                Utils.copyProperties(userDto, user);
                user.setRole(userDto.getRole());

                if (userDto.getStationUid() != null) {
                    Optional<PoliceStation> stationOpt = policeStationRepository.findByUid(userDto.getStationUid());
                    if (stationOpt.isEmpty()) {
                        log.warn("Station not found for UID: {}", userDto.getStationUid());
                        return Response.error("Station not found");
                    }
                    user.setStation(stationOpt.get());
                }
                user.update();
            } else {
                if (userDto.getRole() == Role.CITIZEN) {
                    return Response.error("Use normal registration for citizens");
                }

                if (userDto.getPhoneNumber() == null || userDto.getPhoneNumber().trim().isEmpty()) {
                    return Response.error("Phone number is required");
                }

                Utils.copyProperties(userDto, user);
                user.setRole(userDto.getRole());
                user.setVerified(true);

                if (userDto.getStationUid() != null) {
                    Optional<PoliceStation> stationOpt = policeStationRepository.findByUid(userDto.getStationUid());
                    if (stationOpt.isEmpty()) {
                        log.warn("Station not found for UID: {}", userDto.getStationUid());
                        return Response.error("Station not found");
                    }
                    user.setStation(stationOpt.get());
                }
            }

            try {
                User savedUser = userRepository.save(user);
                log.info("Successfully registered special user with phone: {}", savedUser.getPhoneNumber());
                return Response.success(savedUser);
            } catch (Exception e) {
                log.error("Failed to register special user: {}", e.getMessage());
                return Response.error("Failed to register user: " + e.getMessage());
            }
        }

        public ResponsePage<User> getUsersByStation(PageableParam pageableParam, String stationUid) {
            return new ResponsePage<>(userRepository.getUsersByStation(pageableParam.getPageable(true), pageableParam.getIsActive(), pageableParam.key(), stationUid));
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