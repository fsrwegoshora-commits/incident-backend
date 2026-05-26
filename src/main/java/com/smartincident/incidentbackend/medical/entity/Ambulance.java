package com.smartincident.incidentbackend.medical.entity;

import com.smartincident.incidentbackend.emergency.entity.EmergencyVehicle;
import com.smartincident.incidentbackend.enums.VehicleType;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

/**
 * An ambulance vehicle managed by a hospital or medical emergency unit.
 * Extends the shared {@link EmergencyVehicle} base (JOINED inheritance).
 *
 * Permitted vehicle types: AMBULANCE, ADVANCED_AMBULANCE.
 */
@Entity
@Table(name = "ambulances")
@DiscriminatorValue("AMBULANCE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ambulance extends EmergencyVehicle {

    public static final Set<VehicleType> ALLOWED_TYPES = Set.of(
            VehicleType.AMBULANCE,
            VehicleType.ADVANCED_AMBULANCE
    );

    /**
     * The hospital that owns and operates this ambulance.
     * In Tanzania every hospital manages its own ambulance fleet.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_hospital_id", nullable = false)
    private Hospital baseHospital;
}
