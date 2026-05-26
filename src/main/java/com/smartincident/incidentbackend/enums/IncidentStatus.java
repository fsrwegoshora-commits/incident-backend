package com.smartincident.incidentbackend.enums;

public enum IncidentStatus {
    REPORTED,             // Citizen submitted; awaiting dispatcher review
    UNDER_REVIEW,         // Dispatcher opened and is actively reviewing
    CLASSIFIED,           // Dispatcher assessed; required services determined
    WAITING_FOR_DISPATCH, // Queued; ready for unit assignment
    DISPATCHED,           // Units assigned; responders notified
    ACKNOWLEDGED,         // Responders acknowledged their assignment
    EN_ROUTE,             // Responders heading to the scene
    AT_SCENE,             // Responders arrived at the scene
    IN_PROGRESS,          // Active response underway
    RESOLVED,             // Incident has been resolved
    CLOSED,               // Incident closed and archived
    REJECTED,             // Incident rejected / invalid
    PENDING               // Legacy; DB backward compatibility — equivalent to REPORTED
}