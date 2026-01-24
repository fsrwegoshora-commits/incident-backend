package com.smartincident.incidentbackend.incident.dto;

import com.smartincident.incidentbackend.enums.AgencyResponseStatus;
import com.smartincident.incidentbackend.enums.AgencyRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentAgencyDto {
    private String uid;
    private String incidentUid;
    private String agencyCode;
    private AgencyRole agencyRole;
    private AgencyResponseStatus responseStatus;
    private String responsibleOfficerUid;
    private LocalDateTime notifiedAt;
    private LocalDateTime respondedAt;
    private LocalDateTime arrivedAt;
    private LocalDateTime completedAt;
    private String statusNotes;
}
