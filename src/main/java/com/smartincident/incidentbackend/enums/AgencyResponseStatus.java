package com.smartincident.incidentbackend.enums;

public enum AgencyResponseStatus {
    NOTIFIED,    // Agency got notification
    RESPONDING,  // Agency en route
    ON_SITE,     // Agency at location
    COMPLETED,   // Agency finished
    REJECTED,    // Agency declined
    CANCELLED    // Incident cancelled
}
