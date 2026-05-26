package com.smartincident.incidentbackend.enums;

/**
 * Administrative / operational level of an Emergency Unit.
 * Applies across all agency types (Police, Fire, Medical).
 */
public enum UnitLevel {
    NATIONAL,       // National headquarters / command
    ZONAL,          // Zonal command (police only currently)
    REGIONAL,       // Regional command / headquarters
    DISTRICT,       // District level
    MUNICIPAL,      // Municipality / town council
    WARD,           // Ward / sub-district (police posts, fire brigades)
    HOSPITAL_BASED  // For hospital ambulance dispatch units
}
