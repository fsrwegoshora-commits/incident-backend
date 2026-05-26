package com.smartincident.incidentbackend.fire.entity;

import com.smartincident.incidentbackend.authotp.entity.User;
import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.enums.FireRank;
import jakarta.persistence.*;
import lombok.*;

/**
 * Personnel of the Tanzania Fire and Rescue Force (TFRF).
 *
 * TFRF is an independent government agency under the Ministry of Home Affairs
 * (MoHA) — separate from the police force.  Officers are assigned to a Fire
 * EmergencyUnit (FIRE_BRIGADE, FIRE_DISTRICT_STATION, FIRE_REGIONAL_COMMAND,
 * FIRE_NATIONAL_HQ, or AIRPORT_FIRE_UNIT).
 *
 * TFRF Rank structure (simplified):
 *   Chief Fire Officer (CFO) → Deputy CFO → Senior Divisional Officer →
 *   Divisional Officer → Station Officer → Sub-Officer → Leading Fireman →
 *   Fireman
 */
@Entity
@Table(name = "fire_officers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FireOfficer extends BaseEntity {

    /** TFRF service number (unique per officer). */
    @Column(unique = true, nullable = false)
    private String serviceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FireRank rank;

    /** Fire station / command this officer is assigned to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emergency_unit_id", nullable = false)
    private EmergencyUnit emergencyUnit;

    /** One-to-one link to the system user account. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User userAccount;

    /** Whether the officer is currently on duty / on shift. */
    @Column(nullable = false)
    private Boolean isOnDuty = false;
}
