package com.smartincident.incidentbackend.enums;

public enum Role {
    ROOT,
    AGENCY_ADMIN,
    STATION_ADMIN,
    DISPATCH_CENTER_ADMIN,
    DISPATCHER_SUPERVISOR,
    DISPATCHER,
    POLICE_OFFICER,
    FIRE_OFFICER,
    MEDIC,
    CITIZEN,

    /** @deprecated Renamed to {@link #AGENCY_ADMIN}. DB records migrated on startup. */
    @Deprecated AGENCY_REP,

    /** @deprecated Merged into {@link #STATION_ADMIN}. DB records migrated on startup. */
    @Deprecated FIRE_STATION_ADMIN,

    /** @deprecated Merged into {@link #STATION_ADMIN}. DB records migrated on startup. */
    @Deprecated MEDICAL_STATION_ADMIN
}
