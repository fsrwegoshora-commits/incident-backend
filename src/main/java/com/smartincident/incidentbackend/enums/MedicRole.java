package com.smartincident.incidentbackend.enums;

/**
 * Role of an ambulance crew member in Tanzania.
 */
public enum MedicRole {
    EMT_BASIC,          // Emergency Medical Technician – Basic
    EMT_INTERMEDIATE,   // Emergency Medical Technician – Intermediate
    PARAMEDIC,          // Advanced paramedic (ALS-capable)
    CLINICAL_OFFICER,   // Clinical Officer deployed as crew
    NURSE,              // Registered Nurse deployed as crew
    DRIVER_MEDIC        // Trained ambulance driver with basic first-aid
}
