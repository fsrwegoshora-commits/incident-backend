package com.smartincident.incidentbackend.incident.dto;

import com.smartincident.incidentbackend.incident.entity.IncidentAgency;
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
public class CommandStructureDto {

    private String incidentUid;
    private String incidentTitle;
    private String commanderUid;
    private String commanderName;
    private String commanderRole;
    private String commanderPhone;
    private String leadAgencyUid;
    private String leadAgencyName;
    private String leadAgencyType;
    private LocalDateTime commanderDesignatedAt;
    private Boolean eocActivated;
    private Boolean isMajorIncident;
    private List<AgencySummary> involvedAgencies;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgencySummary {
        private String agencyUid;
        private String agencyName;
        private String agencyType;
        private String role;
        private String responseStatus;
        private LocalDateTime notifiedAt;
        private LocalDateTime respondedAt;
    }
}
