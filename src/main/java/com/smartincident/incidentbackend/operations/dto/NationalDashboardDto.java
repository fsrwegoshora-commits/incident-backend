package com.smartincident.incidentbackend.operations.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NationalDashboardDto {

    private long totalActiveIncidents;
    private long totalActiveVehicles;
    private long totalOnDutyPersonnel;
    private long criticalIncidents;
    private long slaBreached;
    private long majorIncidents;
    private long eocActivated;

    private Map<String, Long> incidentsByStatus;
    private Map<String, Long> incidentsByLevel;
    private Map<String, Long> incidentsByType;

    private List<AgencyStatus> agencyStatuses;
    private List<RegionalSummary> regionalSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgencyStatus {
        private String agencyUid;
        private String agencyName;
        private String agencyType;
        private long activeIncidents;
        private long criticalIncidents;
        private long slaBreached;
        private int onDutyPersonnel;
        private int availableVehicles;
        private int dispatchedVehicles;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegionalSummary {
        private String region;
        private long activeIncidents;
        private long critical;
        private long resolved24h;
    }
}
