package com.smartincident.incidentbackend.utils;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.authotp.security.JwtAuthInterceptor;
import com.smartincident.incidentbackend.authotp.service.UserService;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.permission.service.PermissionService;
import com.smartincident.incidentbackend.police.entity.PoliceOfficer;
import com.smartincident.incidentbackend.police.repository.PoliceOfficerRepository;

public class LoggedUser {

    /** Resolved once per request by JwtAuthFilter and cleared at the end of the request. */
    private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();

    /** Called by JwtAuthFilter right after token validation so get() doesn't re-resolve on every call. */
    public static void setCurrent(User user) {
        CURRENT_USER.set(user);
    }

    /** Called by JwtAuthFilter in a finally block to avoid leaking between requests on the same thread. */
    public static void clearCurrent() {
        CURRENT_USER.remove();
    }

    public static User get() {
        User cached = CURRENT_USER.get();
        if (cached != null) return cached;
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
        return user != null ? user.getUid() : "SYSTEM";
    }

    public static String getName() {
        User user = get();
        return user != null ? user.getName() : null;
    }

    public static Role getRole() {
        User user = get();
        return user != null ? user.getRole() : null;
    }

    public static String getAgencyUid() {
        User user = get();
        return user != null && user.getAgency() != null ? user.getAgency().getUid() : null;
    }

    public static String getEmergencyUnitUid() {
        User user = get();
        return user != null && user.getEmergencyUnit() != null ? user.getEmergencyUnit().getUid() : null;
    }

    public static String getPoliceStationUid() {
        User user = get();
        return user != null && user.getPoliceStation() != null ? user.getPoliceStation().getUid() : null;
    }

    public static String getAnyStationUid() {
        String policeUid = getPoliceStationUid();
        return policeUid != null ? policeUid : getEmergencyUnitUid();
    }

    /** @deprecated Use {@link #getPoliceStationUid()} or {@link #getEmergencyUnitUid()} */
    @Deprecated
    public static String getStationUid() {
        return getAnyStationUid();
    }

    public static boolean isRoot() {
        return getRole() == Role.ROOT;
    }

    public static boolean isAgencyAdmin() {
        return getRole() == Role.AGENCY_ADMIN;
    }

    public static boolean isStationAdmin() {
        return getRole() == Role.STATION_ADMIN;
    }

    public static boolean isDispatcher() {
        return getRole() == Role.DISPATCHER;
    }

    /** Returns the logged-in user's preferred language code ("en", "sw"). Defaults to "en". */
    public static String getPreferredLanguage() {
        User user = get();
        if (user == null || user.getPreferredLanguage() == null) return "en";
        return user.getPreferredLanguage();
    }

    public static boolean hasPermission(Permission permission) {
        Role role = getRole();
        if (role == null) return false;
        try {
            PermissionService permissionService = SpringContext.getBean(PermissionService.class);
            return permissionService.getPermissionsForRole(role).contains(permission);
        } catch (Exception e) {
            return false;
        }
    }

    public static String getOfficerUid() {
        User user = get();
        if (user == null) return null;
        try {
            PoliceOfficerRepository officerRepository = SpringContext.getBean(PoliceOfficerRepository.class);
            PoliceOfficer officer = officerRepository.findByUserAccount(user);
            return officer != null ? officer.getUid() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
