package com.smartincident.incidentbackend.police.dto;

import com.smartincident.incidentbackend.enums.AppointmentPosition;
import com.smartincident.incidentbackend.enums.AppointmentStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDto {
    private String uid;             // for updates
    private String officerUid;
    private String stationUid;
    private AppointmentPosition position;
    private AppointmentStatus status;
    private LocalDate startDate;
    private LocalDate endDate;      // optional
    private String notes;
}
