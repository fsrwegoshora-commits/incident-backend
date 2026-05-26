package com.smartincident.incidentbackend.emergency.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.enums.UnitLevel;
import com.smartincident.incidentbackend.enums.UnitType;
import com.smartincident.incidentbackend.police.entity.Location;
import com.smartincident.incidentbackend.setting.entity.AdministrativeArea;
import com.smartincident.incidentbackend.setting.entity.Agency;
import jakarta.persistence.*;
import lombok.*;

/**
 * Central operational unit for all emergency agencies.
 *
 * Replaces the police-only PoliceStation with a unified entity that covers:
 *   - Tanzania Police Force  (TZP) stations at every administrative level
 *   - Tanzania Fire and Rescue Force (TFRF) – independent agency under MoHA
 *   - Hospital-based ambulance dispatch units (each hospital owns its fleet)
 *
 * The self-referencing parent_unit_id supports the full hierarchy:
 *   Police : Post → Station → District HQ → Regional HQ → Zonal HQ → National HQ
 *   Fire   : Brigade → District Station → Regional Command → National HQ
 *              + Airport Fire Unit (standalone, under Tanzania Airports Authority)
 *   Medical: HospitalAmbulanceUnit (linked to a Hospital, no deeper hierarchy needed)
 */
@Entity
@Table(name = "emergency_units")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"parentUnit", "administrativeArea", "hibernateLazyInitializer", "handler"})
public class EmergencyUnit extends BaseEntity {

    // ── Core identity ────────────────────────────────────────────────────

    @Column(nullable = false)
    private String name;

    /** Which agency this unit belongs to (POLICE, FIRE, MEDICAL). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    /** Structural type of this unit (e.g. POLICE_STATION, FIRE_REGIONAL_COMMAND). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnitType unitType;

    /** Operational / administrative level (NATIONAL, REGIONAL, DISTRICT, …). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UnitLevel level;

    // ── Hierarchy ────────────────────────────────────────────────────────

    /**
     * Parent unit in the chain of command.
     * null → this is the top-level HQ for its agency.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_unit_id")
    @JsonIgnoreProperties({"parentUnit"})
    private EmergencyUnit parentUnit;

    // ── Location ─────────────────────────────────────────────────────────

    @Embedded
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrative_area_id")
    private AdministrativeArea administrativeArea;

    // ── Contact ──────────────────────────────────────────────────────────

    private String contactInfo;

    // ── Computed distance (transient, set by service layer) ──────────────

    @Transient
    private Double temporaryDistance;
}
