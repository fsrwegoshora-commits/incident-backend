package com.smartincident.incidentbackend.emergency.dto;

import com.smartincident.incidentbackend.enums.ShiftTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleShiftDto {
    private String uid;               // null = create, non-null = update

    private String vehicleUid;
    private LocalDate shiftDate;
    private ShiftTime shiftTime;
    private LocalTime startTime;
    private LocalTime endTime;
    private String dutyDescription;

    private List<String> crewUids;    // PoliceOfficer UIDs
    private String standbyLocationUid; // TrafficCheckpoint UID (optional)
}
