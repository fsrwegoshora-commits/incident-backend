package com.smartincident.incidentbackend.authotp.controller;

import com.smartincident.incidentbackend.authotp.dto.UserDto;
import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.repository.UserRepository;
import com.smartincident.incidentbackend.authotp.security.Authenticated;
import com.smartincident.incidentbackend.authotp.security.AuthorizedRole;
import com.smartincident.incidentbackend.authotp.security.JwtAuthInterceptor;
import com.smartincident.incidentbackend.authotp.service.UserService;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.police.dto.OfficerShiftDto;
import com.smartincident.incidentbackend.police.entity.OfficerShift;
import com.smartincident.incidentbackend.police.repository.OfficerShiftRepository;
import com.smartincident.incidentbackend.police.repository.PoliceOfficerRepository;
import com.smartincident.incidentbackend.utils.*;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Controller
@GraphQLApi
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;
    private final JwtAuthInterceptor jwtAuthInterceptor;
    private final PoliceOfficerRepository policeOfficerRepository;
    private final UserRepository userRepository;
    private final OfficerShiftRepository officerShiftRepository;

    @GraphQLMutation(name = "userRegistration", description = "New user is registered or update old one")
    public Response<User> userRegistration(@GraphQLArgument(name = "userDto") UserDto userDto) {
        return userService.userRegistration(userDto);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN,Role.ROOT})
    @GraphQLMutation(name = "registerSpecialUser", description = "Registers a user with a special role (e.g., ADMIN, POLICE_OFFICER)")
    public Response<User> registerSpecialUser(@GraphQLArgument(name = "userDto") UserDto userDto) {
        log.info("Attempting to register special user with phone: {}", userDto.getPhoneNumber());
        return userService.registerSpecialUser(userDto);
    }

    @GraphQLQuery(name = "getUser", description = "Gets a user using it's uid")
    public Response<User> getUser(@GraphQLArgument(name = "uid") String uid) {
        return userService.getUser(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN,Role.ROOT})
    @GraphQLMutation(name = "deleteUser", description = "Deletes a user using it's uid")
    public Response<User> deleteUser(@GraphQLArgument(name = "uid") String uid) {
        return userService.deleteUser(uid);
    }

    @Authenticated
    @AuthorizedRole({Role.CITIZEN})
    @GraphQLMutation(name = "deleteMyAccount", description = "Allows a citizen to delete their own account")
    public Response<User> deleteMyAccount() {
        try {
            String phoneNumber = jwtAuthInterceptor.getValidatedPhoneNumber();
            String currentToken = jwtAuthInterceptor.extractTokenFromRequest();

            if (phoneNumber == null || currentToken == null) {
                return Response.error("Invalid or missing authentication token");
            }

            return userService.deleteOwnAccount(phoneNumber, currentToken);

        } catch (Exception e) {
            log.error("Error in deleteMyAccount: {}", e.getMessage());
            return Response.error("Authentication failed: " + e.getMessage());
        }
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN,Role.ROOT})
    @GraphQLQuery(name = "getUsers", description = "Gets a page of users")
    public ResponsePage<User> getUsers(@GraphQLArgument(name = "pageableParam") PageableParam pageableParam) {
        return userService.getUsers(pageableParam != null ? pageableParam : new PageableParam());
    }

    @GraphQLQuery(name = "me")
    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.POLICE_OFFICER, Role.CITIZEN,Role.ROOT})
    public Response<UserDto> getCurrentUser() {
        String phone = jwtAuthInterceptor.extractPhoneFromRequest();
        if (phone == null) {
            return Response.error("Invalid or missing authentication token");
        }

        Response<User> userResponse = userService.getUserByPhone(phone);
        User user = userResponse.getData();
        if (user == null) {
            return Response.error("User not found for the provided phone number");
        }

        UserDto dto = new UserDto();
        dto.setUid(user.getUid());
        dto.setName(user.getName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRole(user.getRole());
        dto.setStationUid(
                user.getStation() != null ? user.getStation().getUid() : null
        );
        dto.setStationName(
                user.getStation() != null ? user.getStation().getName() : null
        );

        if (user.getRole() == Role.POLICE_OFFICER) {
            policeOfficerRepository.findByUserUidAndIsActiveTrue(dto.getUid()).ifPresent(officer -> {
                dto.setBadgeNumber(officer.getBadgeNumber());
                dto.setRank(officer.getCode());
            });
        }
        if (user.getRole() == Role.POLICE_OFFICER) {
            policeOfficerRepository.findByUserUidAndIsActiveTrue(dto.getUid()).ifPresent(officer -> {
                dto.setBadgeNumber(officer.getBadgeNumber());
                dto.setRank(officer.getCode());
                dto.setOfficerUid(officer.getUid());

                // Check if officer is currently on duty
                LocalDate today = LocalDate.now();
                LocalTime now = LocalTime.now();

                Optional<OfficerShift> currentShiftOpt = officerShiftRepository.findByOfficerUidAndShiftDateAndStartTimeBeforeAndEndTimeAfter(
                        officer.getUid(), today, now, now
                );

                if (currentShiftOpt.isPresent()) {
                    dto.setIsOnDuty(true);
                    OfficerShift shift = currentShiftOpt.get();
                    dto.setCurrentShift(new OfficerShiftDto(shift));
                } else {
                    dto.setIsOnDuty(false);
                }
            });
        }
        return new Response<>(dto);
    }

    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN,Role.ROOT})
    @GraphQLQuery(name = "getUsersByStation", description = "Gets a page of users")
    public ResponsePage<User> getUsersByStation(@GraphQLArgument(name = "pageableParam") PageableParam pageableParam, @GraphQLArgument(name = "policeStationUid") String policeStationUid) {
        return userService.getUsersByStation(pageableParam != null ? pageableParam : new PageableParam(), policeStationUid);
    }

    @GraphQLQuery(name = "getSpecialUsers")
    @Authenticated
    @AuthorizedRole({Role.STATION_ADMIN, Role.ROOT})
    public ResponseList<User> getSpecialUsers(@GraphQLArgument(name = "role") Role role) {
        List<Role> allowedRoles = List.of(Role.POLICE_OFFICER, Role.STATION_ADMIN, Role.ROOT);

        if (role != null && !allowedRoles.contains(role)) {
            return ResponseList.error("Invalid role for special users");
        }

        String stationUid = LoggedUser.getStationUid();
        boolean isRoot = LoggedUser.isRoot();

        List<User> users;
        if (role != null) {
            users = isRoot
                    ? userRepository.findByRoleAndIsActiveTrue(role)
                    : userRepository.findByRoleAndStationUidAndIsActiveTrue(role, stationUid);
        } else {
            users = isRoot
                    ? userRepository.findByRoleInAndIsActiveTrue(allowedRoles)
                    : userRepository.findByRoleInAndStationUidAndIsActiveTrue(allowedRoles, stationUid);
        }

        return new ResponseList<>(users);
    }


}