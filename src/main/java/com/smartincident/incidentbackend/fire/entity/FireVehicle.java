package com.smartincident.incidentbackend.fire.entity;

import com.smartincident.incidentbackend.emergency.entity.EmergencyVehicle;
import com.smartincident.incidentbackend.enums.VehicleType;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

/**
 * A fire-service vehicle operated by the Tanzania Fire and Rescue Force (TFRF).
 * Extends the shared {@link EmergencyVehicle} base (JOINED inheritance).
 *
 * Permitted vehicle types: FIRE_TRUCK, WATER_TENDER, LADDER_TRUCK, RESCUE_TRUCK, COMMAND_VEHICLE.
 */
@Entity
@Table(name = "fire_vehicles")
@DiscriminatorValue("FIRE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FireVehicle extends EmergencyVehicle {

    public static final Set<VehicleType> ALLOWED_TYPES = Set.of(
            VehicleType.FIRE_TRUCK,
            VehicleType.WATER_TENDER,
            VehicleType.LADDER_TRUCK,
            VehicleType.RESCUE_TRUCK,
            VehicleType.COMMAND_VEHICLE
    );

    public static final Set<VehicleType> WATER_CARRYING_TYPES = Set.of(
            VehicleType.FIRE_TRUCK,
            VehicleType.WATER_TENDER
    );

    /** Water tank capacity in litres — only for FIRE_TRUCK and WATER_TENDER. */
    @Column
    private Integer waterCapacityLitres;

    /** Comma-separated list of carried equipment (hoses, cutters, breathing apparatus, etc.). */
    @Column(columnDefinition = "TEXT")
    private String equipmentList;
}
