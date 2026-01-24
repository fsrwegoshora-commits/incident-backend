package com.smartincident.incidentbackend.enums;

public enum AgencyType {
    POLICE("Police", "Law Enforcement"),
    FIRE("Fire & Rescue", "Fire Fighting & Rescue"),
    AMBULANCE("Ambulance", "Emergency Medical Services"),
    MEDICAL("Medical", "Medical Services"),
    DISASTER("Disaster Management", "Disaster Management"),
    OTHER("Other", "Other Emergency Services");

    private final String displayName;
    private final String description;

    AgencyType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}