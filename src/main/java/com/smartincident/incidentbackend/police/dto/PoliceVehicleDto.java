package com.smartincident.incidentbackend.police.dto;

import com.smartincident.incidentbackend.enums.PoliceVehicleType;
import com.smartincident.incidentbackend.enums.VehicleStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoliceVehicleDto {
    /** UID of vehicle to update (null = create). */
    private String uid;
    private String plateNumber;
    private String model;
    private PoliceVehicleType vehicleType;
    private VehicleStatus status;
    private String policeStationUid;
    private String driverName;
    private Integer year;
    private String notes;
}
