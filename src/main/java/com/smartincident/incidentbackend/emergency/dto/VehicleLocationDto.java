package com.smartincident.incidentbackend.emergency.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleLocationDto {
    private String vehicleUid;       // sent by device
    private Double latitude;
    private Double longitude;
    private Double speedKmh;
    private Double heading;          // degrees 0–360
    private String incidentUid;      // non-null when responding to an incident
}
