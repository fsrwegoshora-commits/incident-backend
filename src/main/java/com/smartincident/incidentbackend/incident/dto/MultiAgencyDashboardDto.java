package com.smartincident.incidentbackend.incident.dto;

import com.smartincident.incidentbackend.enums.AgencyResponseStatus;
import com.smartincident.incidentbackend.enums.AgencyRole;
import com.smartincident.incidentbackend.enums.EmergencyLevel;
import com.smartincident.incidentbackend.enums.IncidentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MultiAgencyDashboardDto {

    private String incidentUid;
    private String incidentTitle;
    private IncidentStatus incidentStatus;
    private EmergencyLevel emergencyLevel;
    private boolean isMajorIncident;
    private boolean eocActivated;
    private String location;
    private LocalDateTime reportedAt;
    private LocalDateTime slaDeadline;
    private LocalDateTime slaBreachedAt;
    private String incidentCommanderName;

    private List<AgencyCoordinationEntry> agencies;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AgencyCoordinationEntry {
        private String agencyUid;
        private String agencyName;
        private String agencyCode;
        private AgencyRole role;
        private AgencyResponseStatus responseStatus;
        private String responsibleOfficerName;
        private LocalDateTime notifiedAt;
        private LocalDateTime respondedAt;
        private LocalDateTime arrivedAt;
        private LocalDateTime completedAt;
        private String statusNotes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DashboardSummary {
        private long totalActive;
        private long majorIncidents;
        private long eocActivated;
        private long slaBreached;
        private long multiAgencyEngaged;
        private List<AgencyResponseSummary> agencyResponses;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AgencyResponseSummary {
        private String agencyCode;
        private String agencyName;
        private long notified;
        private long responding;
        private long completed;
        private long unavailable;
    }
}
