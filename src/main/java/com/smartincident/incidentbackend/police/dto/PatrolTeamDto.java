package com.smartincident.incidentbackend.police.dto;

import com.smartincident.incidentbackend.enums.PatrolTeamStatus;
import lombok.Data;

import java.util.List;

@Data
public class PatrolTeamDto {
    private String uid;              // null = create, non-null = update
    private String name;
    private String stationUid;       // defaults to logged-in station admin's station on create
    private String vehicleUid;       // optional
    private String teamLeaderUid;    // optional
    private String driverUid;        // optional
    private List<String> memberUids; // optional
    private String coverageArea;     // optional free text
    private PatrolTeamStatus status;
    private String notes;
}
