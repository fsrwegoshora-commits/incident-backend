package com.smartincident.incidentbackend.dispatcher.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispatchCenterPerformanceDto {

    private String centerUid;
    private String centerName;

    private long totalDispatchers;
    private long totalSupervisors;
    private long onDutyNow;

    private long totalIncidentsHandled;
    private long incidentsResolved;
    private long incidentsPending;
    private long slaBreachedIncidents;

    private Double avgDispatchTimeMinutes;
    private Double avgResponseTimeMinutes;
    private Double avgResolutionTimeMinutes;

    private List<DispatcherMetric> dispatcherMetrics;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DispatcherMetric {
        private String dispatcherUid;
        private String dispatcherName;
        private String role;
        private long activeIncidents;
        private long resolvedToday;
        private boolean onDuty;
    }
}
