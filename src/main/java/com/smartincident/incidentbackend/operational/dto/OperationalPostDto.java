package com.smartincident.incidentbackend.operational.dto;

import com.smartincident.incidentbackend.enums.PostStatus;
import com.smartincident.incidentbackend.enums.PostType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationalPostDto {
    private String uid;
    private String name;
    private PostType postType;
    private PostStatus status;
    private String agencyUid;
    private String parentStationUid;
    private String parentUnitUid;
    private String parentHospitalUid;
    private Double latitude;
    private Double longitude;
    private String address;
    private Double coverageRadiusKm;
    private String notes;
}
