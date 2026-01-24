package com.smartincident.incidentbackend.incident.dto;

import com.smartincident.incidentbackend.enums.EmergencyLevel;
import com.smartincident.incidentbackend.enums.IncidentStatus;
import com.smartincident.incidentbackend.enums.IncidentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentReportDto {

    private String uid;

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
    private Boolean isLiveCallRequested;

    // Relations
    private String reportedByUid;
    private String assignedStationUid;
    private String assignedOfficerUid;

    // Timestamps
    private LocalDateTime reportedAt;
    private LocalDateTime resolvedAt;

    private String leadAgencyCode; // e.g., "POLICE" (default)
    private EmergencyLevel emergencyLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private List<String> involvedAgencyCodes;
}