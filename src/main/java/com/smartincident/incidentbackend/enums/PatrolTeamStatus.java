package com.smartincident.incidentbackend.enums;

public enum PatrolTeamStatus {
    STANDBY,     // At station, ready to deploy
    ON_DUTY,     // Currently deployed / patrolling
    RESPONDING,  // Dispatched to an active incident
    RETURNING,   // Heading back to station
    OFF_DUTY,    // End of shift
    DISBANDED    // Team deactivated
}
