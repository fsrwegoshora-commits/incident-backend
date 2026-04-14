package com.smartincident.incidentbackend.enums;

public enum VehicleStatus {
    AVAILABLE,      // At station, ready to deploy
    DISPATCHED,     // Assigned to active incident
    EN_ROUTE,       // Moving to incident scene
    ON_SCENE,       // At incident location
    RETURNING,      // Heading back to station
    MAINTENANCE,    // Under service / repair
    OUT_OF_SERVICE  // Permanently unavailable
}
