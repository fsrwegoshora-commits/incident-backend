package com.smartincident.incidentbackend.incident.dto;

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
public class IncidentTimelineDto {

    private String incidentUid;
    private String title;
    private String type;
    private String location;
    private String currentStatus;
    private String emergencyLevel;
    private String emergencyCategory;

    // Assigned responders
    private String assignedPoliceStation;
    private String assignedUnit;
    private String assignedOfficer;
    private String assignedDispatcher;

    // Key timestamps
    private LocalDateTime reportedAt;
    private LocalDateTime dispatchedAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime enRouteAt;
    private LocalDateTime atSceneAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private LocalDateTime slaDeadline;
    private Boolean slaBreached;

    // Chronological event log
    private List<TimelineEvent> events;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineEvent {
        private LocalDateTime timestamp;
        private String eventType;
        private String description;
        private String actorName;
        private String actorRole;
        private String fromStatus;
        private String toStatus;
        private String note;
    }
}
