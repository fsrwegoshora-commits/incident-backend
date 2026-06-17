package com.smartincident.incidentbackend.fire.dto;

import com.smartincident.incidentbackend.enums.FireCrewStatus;
import lombok.Data;

import java.util.List;

@Data
public class FireCrewDto {
    private String uid;
    private String name;
    private String unitUid;
    private String vehicleUid;
    private String crewCommanderUid;
    private String driverUid;
    private List<String> memberUids;
    private String coverageArea;
    private FireCrewStatus status;
    private String notes;
}
