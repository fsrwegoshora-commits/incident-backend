package com.smartincident.incidentbackend.police.dto;

import com.smartincident.incidentbackend.enums.OfficerRank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoliceOfficerDto {
    private String uid;
    private String badgeNumber;
    private OfficerRank code;
    private String stationUid;
    private String userUid;
}
