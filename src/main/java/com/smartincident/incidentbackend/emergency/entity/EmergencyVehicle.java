package com.smartincident.incidentbackend.emergency.entity;

import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.enums.VehicleStatus;
import com.smartincident.incidentbackend.enums.VehicleType;
import com.smartincident.incidentbackend.police.entity.PoliceStation;
import com.smartincident.incidentbackend.setting.entity.Department;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyVehicle extends BaseEntity {

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

    // ── Fire-truck specifics (null for ambulances) ──────────────────────────
    @Column
    private Integer waterCapacityLitres;

    @Column(columnDefinition = "TEXT")
    private String equipmentList;

    // ── Ambulance specifics (null for fire trucks) ──────────────────────────
    @Column(nullable = false)
    private Boolean hasAdvancedLifeSupport = false;

    @Column(nullable = false)
    private Integer stretcherCapacity = 1;

    // ── Assignment ──────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private PoliceStation station;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    // ── Last known GPS position ─────────────────────────────────────────────
    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column
    private LocalDateTime lastLocationUpdate;
}
