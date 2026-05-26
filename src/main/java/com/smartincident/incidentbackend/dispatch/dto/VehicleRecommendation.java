package com.smartincident.incidentbackend.dispatch.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VehicleRecommendation {
    private String uid;
    private String plateNumber;
    private String model;
    private String vehicleType;
    private String status;
    private String unitName;
    private String unitUid;
    private Double distanceKm;
    private Integer etaMinutes;
}
