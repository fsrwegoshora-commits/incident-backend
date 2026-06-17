package com.smartincident.incidentbackend.police.dto;

import lombok.Data;

@Data
public class VehicleAssignmentDto {
    /** PATROL, CHECKPOINT, or RETURN */
    private String assignmentType;
    /** Required when assignmentType = CHECKPOINT */
    private String checkpointUid;
    /** Optional free-text note (patrol area, route, etc.) */
    private String note;
}
