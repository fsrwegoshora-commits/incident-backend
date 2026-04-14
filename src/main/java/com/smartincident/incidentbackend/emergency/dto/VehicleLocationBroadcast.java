package com.smartincident.incidentbackend.emergency.dto;

import com.smartincident.incidentbackend.enums.VehicleStatus;
import com.smartincident.incidentbackend.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Payload broadcast over WebSocket to subscribers watching a vehicle or incident. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleLocationBroadcast {
    private String vehicleUid;
    private String plateNumber;
    private VehicleType vehicleType;
    private VehicleStatus vehicleStatus;
    private Double latitude;
    private Double longitude;
    private Double speedKmh;
    private Double heading;
    private Integer etaMinutes;          // to incident scene (null when not dispatched)
    private String incidentUid;
    private String dispatchStatus;       // current DispatchStatus string
    private LocalDateTime recordedAt;
}
