package com.smartincident.incidentbackend.authotp.dto;

import com.smartincident.incidentbackend.enums.Permission;
import com.smartincident.incidentbackend.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String phoneNumber;
    private String username;
    private Role role;
    private String stationUid;
    private Set<Permission> permissions;

    /** Citizen auth response (OTP login). */
    public AuthResponse(String accessToken, String refreshToken,
                        String phoneNumber, Role role,
                        String stationUid, Set<Permission> permissions) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.stationUid = stationUid;
        this.permissions = permissions;
    }

    /** Management auth response (username/password login). */
    public AuthResponse(String accessToken, String refreshToken,
                        String phoneNumber, String username, Role role,
                        String stationUid, Set<Permission> permissions) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.phoneNumber = phoneNumber;
        this.username = username;
        this.role = role;
        this.stationUid = stationUid;
        this.permissions = permissions;
    }
}
