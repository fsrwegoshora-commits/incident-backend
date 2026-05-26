package com.smartincident.incidentbackend.enums;

/**
 * The three core emergency agency types in Tanzania.
 * Used as a typed code on the Agency entity to replace the free-text String.
 *
 * Maps to the 7 citizen-facing emergency categories:
 *   Police Only              → POLICE
 *   Medical Only             → MEDICAL
 *   Fire Only                → FIRE
 *   Police + Medical         → POLICE + MEDICAL
 *   Police + Fire            → POLICE + FIRE
 *   Medical + Fire           → MEDICAL + FIRE
 *   All Three                → POLICE + FIRE + MEDICAL
 */
public enum AgencyCode {

    /** Tanzania Police Force (TPF) */
    POLICE("Tanzania Police Force", "Law enforcement, crime response, traffic"),

    /** Tanzania Fire and Rescue Force (TFRF) — independent government agency */
    FIRE("Tanzania Fire and Rescue Force", "Fire suppression, rescue, hazmat"),

    /** Emergency Medical Services — hospital-based ambulance fleets */
    MEDICAL("Emergency Medical Services", "Ambulance dispatch, paramedic response, hospital coordination");

    private final String displayName;
    private final String description;

    AgencyCode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
