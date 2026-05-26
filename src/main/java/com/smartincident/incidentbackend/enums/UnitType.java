package com.smartincident.incidentbackend.enums;

/**
 * Type of Emergency Unit.
 *
 * POLICE   — Tanzania Police Force hierarchy
 * FIRE     — Tanzania Fire and Rescue Force (TFRF) — independent agency under MoHA
 * MEDICAL  — Hospital-based ambulance dispatch units
 */
public enum UnitType {

    // ── Police (Tanzania Police Force) ────────────────────────────────────
    POLICE_POST,            // Smallest unit: ward / village post
    POLICE_STATION,         // Main station (most common)
    DISTRICT_POLICE_HQ,     // District Police Headquarters (OCD)
    REGIONAL_POLICE_HQ,     // Regional Police Headquarters (RPC)
    ZONAL_POLICE_HQ,        // Zonal Command (e.g. Lake Zone, Northern Zone)
    NATIONAL_POLICE_HQ,     // Tanzania Police Force HQ (Dar es Salaam)

    // ── Fire (Tanzania Fire and Rescue Force — TFRF) ───────────────────────
    FIRE_BRIGADE,           // Smallest fire unit: ward/town fire post
    FIRE_DISTRICT_STATION,  // District fire station
    FIRE_REGIONAL_COMMAND,  // Regional TFRF Command (26 regions)
    FIRE_NATIONAL_HQ,       // TFRF National Headquarters
    AIRPORT_FIRE_UNIT,      // Airport Fire & Rescue (Tanzania Airports Authority)

    // ── Medical / Ambulance (hospital-based) ──────────────────────────────
    HOSPITAL_AMBULANCE_UNIT, // Dispatch unit linked to a specific hospital

    // ── Dispatch / Coordination ────────────────────────────────────────────
    DISPATCH_CENTER          // Emergency Coordination Center — home of DISPATCHER users
}
