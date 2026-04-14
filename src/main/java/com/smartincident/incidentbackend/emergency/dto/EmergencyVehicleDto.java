package com.smartincident.incidentbackend.emergency.dto;

import com.smartincident.incidentbackend.enums.VehicleStatus;
import com.smartincident.incidentbackend.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyVehicleDto {
    private String uid;               // null = create, non-null = update

    private String plateNumber;
    private String model;
    private VehicleType vehicleType;
    private VehicleStatus status;

    // Fire-truck fields
    private Integer waterCapacityLitres;
    private String equipmentList;

    // Ambulance fields
    private Boolean hasAdvancedLifeSupport;
    private Integer stretcherCapacity;

    // Assignment
    private String stationUid;
    private String departmentUid;
}
