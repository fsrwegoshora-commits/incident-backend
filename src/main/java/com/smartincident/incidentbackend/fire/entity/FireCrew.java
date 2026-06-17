package com.smartincident.incidentbackend.fire.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smartincident.incidentbackend.emergency.entity.EmergencyUnit;
import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.enums.FireCrewStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * A fire response crew — a team of firefighters assigned to a vehicle and a unit.
 * Mirrors the PatrolTeam concept for the Fire & Rescue Force.
 */
@Entity
@Table(name = "fire_crews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FireCrew extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emergency_unit_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private EmergencyUnit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "unit"})
    private FireVehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_commander_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "emergencyUnit", "userAccount"})
    private FireOfficer crewCommander;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "emergencyUnit", "userAccount"})
    private FireOfficer driver;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "fire_crew_members",
        joinColumns = @JoinColumn(name = "fire_crew_id"),
        inverseJoinColumns = @JoinColumn(name = "officer_id")
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "emergencyUnit"})
    @Builder.Default
    private List<FireOfficer> members = new ArrayList<>();

    private String coverageArea;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FireCrewStatus status = FireCrewStatus.STANDBY;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
