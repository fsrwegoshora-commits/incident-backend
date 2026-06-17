package com.smartincident.incidentbackend.operations.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationsDashboardDto {

    private DashboardSummary summary;
    private List<ActiveIncidentSummary> activeIncidents;
    private List<UnitStatus> units;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardSummary {
        private long totalActive;
        private long critical;
        private long high;
        private long medium;
        private long low;
        private long slaBreached;
        private long slaApproaching;
        private long dispatched;
        private long enRoute;
        private long atScene;
        private long inProgress;
        private int onDutyDispatchers;
        private int onDutyOfficers;
        private int onDutyFireOfficers;
        private int onDutyMedics;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveIncidentSummary {
        private String uid;
        private String title;
        private String type;
        private String status;
        private String emergencyLevel;
        private String location;
        private Double latitude;
        private Double longitude;
        private LocalDateTime reportedAt;
        private LocalDateTime slaDeadline;
        private Boolean slaBreached;
        private String assignedDispatcher;
        private String assignedUnit;
        private String assignedPoliceStation;
        private Boolean isMajorIncident;
        private Boolean eocActivated;
        private String incidentCommander;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnitStatus {
        private String unitUid;
        private String unitName;
        private String unitType;
        private String agencyType;
        private int onDutyPersonnel;
        private int activeIncidents;
        private int availableVehicles;
    }
}
