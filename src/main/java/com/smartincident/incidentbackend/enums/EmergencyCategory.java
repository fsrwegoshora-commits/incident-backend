package com.smartincident.incidentbackend.enums;

/**
 * The 7 emergency category combinations a citizen selects when submitting
 * an incident report. Drives which agencies are notified and which units
 * are auto-assigned.
 */
public enum EmergencyCategory {

    /** BR-15: Handled by medical only — no police unless escalated. */
    MEDICAL_ONLY,

    POLICE_ONLY,

    FIRE_ONLY,

    POLICE_MEDICAL,

    POLICE_FIRE,

    MEDICAL_FIRE,

    ALL_THREE
}
