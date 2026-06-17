package com.smartincident.incidentbackend.dispatcher.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmartDispatchRecommendation {

    private String incidentUid;
    private String incidentTitle;
    private Double incidentLat;
    private Double incidentLng;
    private String emergencyCategory;
    private String emergencyLevel;

    private List<UnitRecommendation> policeRecommendations;
    private List<UnitRecommendation> fireRecommendations;
    private List<UnitRecommendation> medicalRecommendations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnitRecommendation {
        private String uid;
        private String name;
        private String unitType;
        private String agencyName;
        private Double distanceKm;
        private Integer etaMinutes;
        private Integer onDutyCrewCount;
        private Integer activeIncidentCount;
        private Integer availableVehicles;
        /** 0–100 composite score. Higher = better recommendation. */
        private Integer score;
        private Double unitLat;
        private Double unitLng;
        private List<String> warnings;
        /** OPERATIONAL_POST | POLICE_STATION | EMERGENCY_UNIT */
        private String resourceType;
        /** UID of the parent OperationalPost, if this recommendation is post-based. */
        private String postUid;
    }
}
