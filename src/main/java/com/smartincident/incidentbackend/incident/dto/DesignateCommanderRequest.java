package com.smartincident.incidentbackend.incident.dto;

import lombok.Data;

@Data
public class DesignateCommanderRequest {
    private String commanderUserUid;
    private String leadAgencyUid;
}
