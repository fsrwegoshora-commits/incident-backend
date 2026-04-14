package com.smartincident.incidentbackend.enums;

public enum StationLevel {
    POLICE_POST,          // Smallest unit (village / ward)
    POLICE_STATION,       // Main station
    DISTRICT_HQ,          // District Police Headquarters
    REGIONAL_HQ,          // Regional Police Headquarters
    ZONAL_SPECIALIZED,    // Zonal / Specialized units (DSZ, CID, Traffic, etc.)
    NATIONAL_HQ           // National Police Headquarters (no parent)
}
