package com.smartincident.incidentbackend.authotp.dto;

import com.smartincident.incidentbackend.enums.DispatcherAppointment;
import com.smartincident.incidentbackend.enums.OfficerRank;
import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.police.dto.OfficerShiftDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String uid;
    private String name;
    private String phoneNumber;
    private String otpCode;
    private Role role;

    /** Username for management users (login credential). */
    private String username;

    /** Plain-text password — only used during creation/update; never stored in plain text. */
    private String password;
    /** UID of the EmergencyUnit to assign this user to (replaces stationUid). */
    private String emergencyUnitUid;
    /** @deprecated use emergencyUnitUid */
    @Deprecated
    private String stationUid;
    // Getters and setters

    // Officer-specific
    private String badgeNumber;
    private OfficerRank rank;
    //private String dutyInfo;

    private String stationName;

    private Boolean isOnDuty;
    private OfficerShiftDto currentShift;

    private String officerUid;

    /** UID of the hospital to assign this user to (for MEDIC role). */
    private String hospitalUid;

    /** Dispatcher appointment/rank — applies to DISPATCH_CENTER_ADMIN, DISPATCHER_SUPERVISOR, DISPATCHER. */
    private DispatcherAppointment appointment;

    /** Permissions granted to this user's role — included in /me and auth responses. */
    private Set<Permission> permissions;

    /** Returns emergencyUnitUid, falling back to stationUid for backward compatibility. */
    public String getEmergencyUnitUid() {
        return emergencyUnitUid != null ? emergencyUnitUid : stationUid;
    }
}
