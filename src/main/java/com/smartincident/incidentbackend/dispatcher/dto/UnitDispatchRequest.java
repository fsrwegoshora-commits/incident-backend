package com.smartincident.incidentbackend.dispatcher.dto;

import lombok.Data;

@Data
public class UnitDispatchRequest {
    /** UID of the police station to dispatch (null to skip police). */
    private String policeStationUid;
    /** UID of the fire emergency unit to dispatch (null to skip fire). */
    private String fireUnitUid;
    /** UID of the hospital ambulance unit to dispatch (null to skip medical). */
    private String medicalUnitUid;
    /** Optional dispatcher notes. */
    private String notes;
    /** Estimated time of arrival in minutes. */
    private Integer etaMinutes;
}
