package com.smartincident.incidentbackend.escalation.dto;

import com.smartincident.incidentbackend.enums.EscalationLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceEscalationDto {
    private String incidentUid;
    private EscalationLevel level;
    /** POLICE, FIRE, MEDICAL, MULTI */
    private String resourceType;
    private String reason;
}
