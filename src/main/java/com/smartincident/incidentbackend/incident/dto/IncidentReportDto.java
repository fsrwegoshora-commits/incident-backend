package com.smartincident.incidentbackend.incident.dto;

import com.smartincident.incidentbackend.enums.IncidentStatus;
import com.smartincident.incidentbackend.enums.IncidentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentReportDto {

    private String uid;  // For updates

    // Basic Info
    private String title;
    private String description;
    private IncidentType type;

    // Location
    private String location;
    private Double latitude;
    private Double longitude;

    // Media
    private String imageUrl;
    private String audioUrl;
    private String videoUrl;

    // Status
    private IncidentStatus status;
    private boolean isLiveCallRequested;

    // Relations
    private String reportedByUid;  // User who reported
    private String assignedStationUid;  // Police station
    private String assignedOfficerUid;  // Assigned officer (optional)

    // Timestamps
    private LocalDateTime reportedAt;
    private LocalDateTime resolvedAt;
}