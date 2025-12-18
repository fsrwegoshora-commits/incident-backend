package com.smartincident.incidentbackend.police.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrafficCheckPointDto {
    private String uid;
    private String name;
    private String contactInfo;
    private Double coverageRadiusKm;
    private String policeStationUid;
    private String departmentUid;
    private Boolean active;
    private LocationDto location;
}
