package com.smartincident.incidentbackend.police.dto;

import com.smartincident.incidentbackend.enums.AppointmentPosition;
import com.smartincident.incidentbackend.enums.OfficerRank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoliceOfficerDto {
    private String uid;
    private String badgeNumber;
    private OfficerRank code;
    private String stationUid;
    private String userUid;            // optional when name + phoneNumber are provided
    private String departmentUid;
    // Inline user creation fields
    private String name;
    private String phoneNumber;
    // Appointment fields (used on create)
    private AppointmentPosition appointmentPosition;
    private LocalDate appointmentStartDate;
    private LocalDate appointmentEndDate;   // optional
    private String appointmentNotes;
}
