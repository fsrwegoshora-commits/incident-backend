package com.smartincident.incidentbackend.enums;

public enum IncidentNature {
    /** Happening now or requires immediate emergency response. Routed to dispatcher queue. */
    EMERGENCY,
    /** Historical report, complaint, or investigation request. Routed to station investigation queue. */
    NON_EMERGENCY
}
