package com.smartincident.incidentbackend.emergency.entity;

import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.enums.VehicleStatus;
import com.smartincident.incidentbackend.enums.VehicleType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Abstract base for all emergency response vehicles.
 * Concrete subtypes:
 *   - {@link com.smartincident.incidentbackend.fire.entity.FireVehicle}  (fire_vehicles table)
 *   - {@link com.smartincident.incidentbackend.medical.entity.Ambulance} (ambulances table)
 *
 * IncidentDispatch, VehicleLocation and VehicleShift reference this base class
 * so they work polymorphically with both subtypes.
 */
@Entity
@Table(name = "emergency_vehicles")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "vehicle_category", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
public abstract class EmergencyVehicle extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String plateNumber;

    @Column(nullable = false)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    /** The EmergencyUnit this vehicle is based at. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emergency_unit_id", nullable = false)
    private EmergencyUnit emergencyUnit;

    // ── Last known GPS position ─────────────────────────────────────────────
    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column
    private LocalDateTime lastLocationUpdate;

    /**
     * Whether this vehicle carries Advanced Life Support (ALS) equipment.
     * Meaningful for ambulances; always false for fire vehicles.
     */
    @Column(nullable = false)
    private Boolean hasAdvancedLifeSupport = false;

    /**
     * Number of stretcher bays.
     * Meaningful for ambulances; 0 for fire vehicles.
     */
    @Column(nullable = false)
    private Integer stretcherCapacity = 0;

    /** Runtime-only: populated by Haversine distance queries, never persisted. */
    @Transient
    private Double temporaryDistance;
}
