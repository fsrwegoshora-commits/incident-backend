package com.smartincident.incidentbackend.dispatch.dto;

import lombok.Builder;
import lombok.Data;

/**
 * A single recommended unit (police station or emergency unit) with its
 * distance and calculated ETA from the incident location.
 */
@Data
@Builder
public class UnitRecommendation {
    private String uid;
    private String name;
    private String type;          // POLICE_STATION | FIRE_UNIT | MEDICAL_UNIT
    private Double distanceKm;
    private Integer etaMinutes;
    private Double latitude;
    private Double longitude;
    private Integer availableVehicles;
}
