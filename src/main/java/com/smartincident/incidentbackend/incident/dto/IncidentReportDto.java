package com.smartincident.incidentbackend.incident.dto;

import com.smartincident.incidentbackend.enums.EmergencyCategory;
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

    /** Medical symptoms (populated for MEDICAL category). */
    private String symptoms;

    // Status
    private IncidentStatus status;
    private Boolean isLiveCallRequested;

    /**
     * Which combination of emergency services the citizen is requesting.
     * Drives requiresPoliceService / requiresFireService / requiresMedicalService flags
     * and auto-assignment logic in the service layer.
     */
    private EmergencyCategory emergencyCategory;

    // Relations
    private String reportedByUid;
    /** UID of the PoliceStation to assign (police incidents). Auto-assigned if null. */
    private String assignedPoliceStationUid;
    /** UID of the EmergencyUnit to assign (fire/medical incidents). Auto-assigned if null. */
    private String assignedUnitUid;
    private String assignedOfficerUid;

    // Major incident / EOC fields
    private String incidentCommanderUid;
    private String incidentCommanderAgencyUid;
    private Boolean isMajorIncident;
    private Boolean eocActivated;

    // Timestamps
    private LocalDateTime reportedAt;
    private LocalDateTime resolvedAt;

    private String leadAgencyCode; // e.g., "POLICE" (default)
    private EmergencyLevel emergencyLevel; // LOW, MEDIUM, HIGH, CRITICAL
    private List<String> involvedAgencyCodes;

    // Multi-service response request flags (auto-set from emergencyCategory if not provided)
    private Boolean requiresPoliceService;
    private Boolean requiresFireService;
    private Boolean requiresMedicalService;
}