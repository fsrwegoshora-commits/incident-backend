package com.smartincident.incidentbackend.medical.dto;

import com.smartincident.incidentbackend.enums.VehicleStatus;
import com.smartincident.incidentbackend.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AmbulanceDto {

    /** Null = create, non-null = update. */
    private String uid;

    private String plateNumber;
    private String model;

    /** Must be one of: AMBULANCE, ADVANCED_AMBULANCE. */
    private VehicleType vehicleType;

    private VehicleStatus status;

    /** Whether this unit carries advanced life support (ALS) equipment. */
    private Boolean hasAdvancedLifeSupport;

    /** Number of stretcher bays (default 1). */
    private Integer stretcherCapacity;

    /** UID of the Hospital that owns this ambulance. Required on create. */
    private String baseHospitalUid;

    /** UID of the EmergencyUnit (HOSPITAL_AMBULANCE_UNIT) this vehicle is based at. */
    private String emergencyUnitUid;
}
