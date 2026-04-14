package com.smartincident.incidentbackend.authotp.dto;

import com.smartincident.incidentbackend.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String phoneNumber;
    private Role role;
    private String stationUid;
}
