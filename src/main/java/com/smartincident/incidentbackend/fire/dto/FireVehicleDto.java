package com.smartincident.incidentbackend.fire.dto;

import com.smartincident.incidentbackend.enums.VehicleStatus;
import com.smartincident.incidentbackend.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FireVehicleDto {

    /** Null = create, non-null = update. */
    private String uid;

    private String plateNumber;
    private String model;

    /** Must be one of: FIRE_TRUCK, WATER_TENDER, LADDER_TRUCK, RESCUE_TRUCK, COMMAND_VEHICLE. */
    private VehicleType vehicleType;

    private VehicleStatus status;

    /** Litres — only applicable to FIRE_TRUCK and WATER_TENDER. */
    private Integer waterCapacityLitres;

    /** Free-text list of equipment carried on this vehicle. */
    private String equipmentList;

    /** UID of the EmergencyUnit (fire brigade / station) this vehicle is based at. */
    private String emergencyUnitUid;
}
