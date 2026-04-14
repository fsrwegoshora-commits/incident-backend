package com.smartincident.incidentbackend.enums;

public enum DispatchStatus {
    PENDING,       // Dispatch created, crew not yet notified
    ACKNOWLEDGED,  // Crew confirmed dispatch
    EN_ROUTE,      // Vehicle departed station
    ON_SCENE,      // Vehicle arrived at incident
    CLEARED,       // Incident resolved, vehicle returning
    CANCELLED      // Dispatch cancelled before departure
}
