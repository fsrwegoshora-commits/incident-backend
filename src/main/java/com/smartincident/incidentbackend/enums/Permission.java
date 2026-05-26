package com.smartincident.incidentbackend.enums;

public enum Permission {
    CREATE_USER(PermissionCategory.USER_MANAGEMENT),
    UPDATE_USER(PermissionCategory.USER_MANAGEMENT),
    DELETE_USER(PermissionCategory.USER_MANAGEMENT),
    ASSIGN_ROLE(PermissionCategory.USER_MANAGEMENT),
    CREATE_INCIDENT(PermissionCategory.INCIDENT_MANAGEMENT),
    UPDATE_INCIDENT(PermissionCategory.INCIDENT_MANAGEMENT),
    CLOSE_INCIDENT(PermissionCategory.INCIDENT_MANAGEMENT),
    DISPATCH_INCIDENT(PermissionCategory.INCIDENT_MANAGEMENT),
    UPLOAD_EVIDENCE(PermissionCategory.INCIDENT_MANAGEMENT),
    VIEW_RESPONDERS(PermissionCategory.INCIDENT_MANAGEMENT),
    VIEW_OWN_INCIDENTS(PermissionCategory.INCIDENT_MANAGEMENT),
    UPDATE_OWN_PROFILE(PermissionCategory.USER_MANAGEMENT),
    MANAGE_VEHICLES(PermissionCategory.RESOURCE_MANAGEMENT),
    VIEW_VEHICLES(PermissionCategory.RESOURCE_MANAGEMENT),
    MANAGE_STATIONS(PermissionCategory.RESOURCE_MANAGEMENT),
    MANAGE_CHECKPOINTS(PermissionCategory.RESOURCE_MANAGEMENT),
    MANAGE_AGENCY(PermissionCategory.ADMINISTRATION),
    VIEW_ANALYTICS(PermissionCategory.ADMINISTRATION),
    VIEW_AUDIT_LOGS(PermissionCategory.ADMINISTRATION);

    private final PermissionCategory category;

    Permission(PermissionCategory category) {
        this.category = category;
    }

    public PermissionCategory getCategory() {
        return category;
    }
}
