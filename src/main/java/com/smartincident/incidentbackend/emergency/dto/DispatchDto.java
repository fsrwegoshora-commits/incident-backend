package com.smartincident.incidentbackend.emergency.dto;

import com.smartincident.incidentbackend.enums.DispatchStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DispatchDto {
    private String incidentUid;
    private String vehicleUid;
    private String notes;

    // Used only for status update endpoint
    private DispatchStatus status;
    private Integer etaMinutes;
}
