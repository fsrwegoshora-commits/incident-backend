package com.smartincident.incidentbackend.enums;

public enum FireCrewStatus {
    STANDBY,     // At station, ready to deploy
    ON_DUTY,     // Actively deployed
    RESPONDING,  // Dispatched to an active incident
    RETURNING,   // Heading back to station
    OFF_DUTY,    // End of shift
    DISBANDED    // Crew deactivated
}
