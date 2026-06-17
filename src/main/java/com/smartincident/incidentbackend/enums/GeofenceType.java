package com.smartincident.incidentbackend.enums;

public enum GeofenceType {
    PROTECTED_ZONE,   // High-security area — alert on any vehicle entry
    RESTRICTED_AREA,  // Off-limits to non-authorised vehicles
    CHECKPOINT,       // Traffic checkpoint or patrol boundary
    PATROL_ZONE,      // Expected operating area for an assigned unit
    RESPONSE_ZONE,    // Defined coverage area for a station or unit
    EXCLUSION_ZONE    // No-go area (crime scene, disaster perimeter)
}
