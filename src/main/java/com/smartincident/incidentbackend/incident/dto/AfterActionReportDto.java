package com.smartincident.incidentbackend.incident.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AfterActionReportDto {
    private String uid;
    private String incidentUid;
    private String executiveSummary;
    private String timeline;
    private String whatWentWell;
    private String whatWentWrong;
    private String rootCauseAnalysis;
    private String recommendedActions;
    private String lessonsLearned;
    private String agencyCoordinationNotes;
}
