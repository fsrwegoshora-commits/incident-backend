package com.smartincident.incidentbackend.authotp.dto;

import com.smartincident.incidentbackend.enums.OfficerRank;
import com.smartincident.incidentbackend.enums.Role;
import com.smartincident.incidentbackend.police.dto.OfficerShiftDto;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String uid;
    private String name;
    private String phoneNumber;
    private String otpCode; // Add this
    private Role role;
    private String stationUid;
    // Getters and setters

    // Officer-specific
    private String badgeNumber;
    private OfficerRank rank;
    //private String dutyInfo;

    private String stationName;

    private Boolean isOnDuty;
    private OfficerShiftDto currentShift;

    @GraphQLQuery(name = "officerUid")
    private String officerUid;

}
