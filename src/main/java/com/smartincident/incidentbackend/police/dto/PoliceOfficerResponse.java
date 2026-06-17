package com.smartincident.incidentbackend.police.dto;

import com.smartincident.incidentbackend.enums.AppointmentPosition;
import com.smartincident.incidentbackend.enums.AppointmentStatus;
import com.smartincident.incidentbackend.enums.OfficerRank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PoliceOfficerResponse {
    private String uid;
    private String badgeNumber;
    private OfficerRank code;
    private String name;
    private String phoneNumber;
    private String stationUid;
    private String stationName;
    private boolean isOnDuty;
    private AppointmentPosition appointmentPosition;
    private AppointmentStatus appointmentStatus;
    private LocalDate appointmentStartDate;
}
