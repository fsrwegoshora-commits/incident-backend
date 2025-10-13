package com.smartincident.incidentbackend.utils;


import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.security.JwtAuthInterceptor;
import com.smartincident.incidentbackend.authotp.service.UserService;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.police.entity.PoliceOfficer;
import com.smartincident.incidentbackend.police.repository.PoliceOfficerRepository;

public class LoggedUser {

    public static User get() {
        try {
            JwtAuthInterceptor jwtAuthInterceptor = SpringContext.getBean(JwtAuthInterceptor.class);
            UserService userService = SpringContext.getBean(UserService.class);

            String phone = jwtAuthInterceptor.extractPhoneFromRequest();
            if (phone == null) return null;

            return userService.getUserByPhone(phone).getData();
        } catch (Exception e) {
            return null;
        }
    }
    public static String getUid() {
        User user = get();
        return user != null ? user.getUid() : "SYSTEM"; // fallback uid
    }

    public static String getName() {
        User user = get();
        return user != null ? user.getName() : null;
    }

    public static Role getRole() {
        User user = get();
        return user != null ? user.getRole() : null;
    }

    public static String getStationUid() {
        User user = get();
        return user != null && user.getStation() != null ? user.getStation().getUid() : null;
    }

    public static boolean isRoot() {
        return getRole() == Role.ROOT;
    }

    public static boolean isStationAdmin() {
        return getRole() == Role.STATION_ADMIN;
    }
    public static String getOfficerUid() {
        User user = get();
        if (user == null) {
            return null;
        }
        try {
            PoliceOfficerRepository officerRepository = SpringContext.getBean(PoliceOfficerRepository.class);
            PoliceOfficer officer = officerRepository.findByUserAccount(user);
            return officer != null ? officer.getUid() : null;
        } catch (Exception e) {
            return null;
        }
    }
}


