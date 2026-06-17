package com.smartincident.incidentbackend.police.entity;

import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.enums.PoliceVehicleType;
import com.smartincident.incidentbackend.enums.VehicleStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "police_vehicles",
    indexes = {
        @Index(name = "idx_pv_station",  columnList = "police_station_id"),
        @Index(name = "idx_pv_status",   columnList = "status"),
        @Index(name = "idx_pv_plate",    columnList = "plate_number")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoliceVehicle extends BaseEntity {

    @Column(name = "plate_number", nullable = false, unique = true, length = 30)
    private String plateNumber;

    @Column(length = 100)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 30)
    private PoliceVehicleType vehicleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private VehicleStatus status = VehicleStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "police_station_id", nullable = false)
    private PoliceStation policeStation;

    @Column(name = "driver_name", length = 150)
    private String driverName;

    @Column(length = 4)
    private Integer year;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
