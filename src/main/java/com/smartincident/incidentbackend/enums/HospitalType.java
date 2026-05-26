package com.smartincident.incidentbackend.enums;

/**
 * Classification of hospitals in Tanzania's health system.
 */
public enum HospitalType {
    NATIONAL_REFERRAL,   // e.g. Muhimbili National Hospital (MNH), KCMC
    REGIONAL_REFERRAL,   // Regional referral hospitals (26 regions)
    DISTRICT,            // District-level government hospital
    HEALTH_CENTRE,       // Health centre with ambulance capacity
    SPECIALIZED,         // Specialized hospitals (e.g. Muhimbili Orthopaedic)
    PRIVATE,             // Private hospitals (Aga Khan, TMJ, etc.)
    FAITH_BASED          // Faith-based hospitals (CCBRT, Selian, etc.)
}
