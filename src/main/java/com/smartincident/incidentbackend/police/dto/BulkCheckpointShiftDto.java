package com.smartincident.incidentbackend.police.dto;

import com.smartincident.incidentbackend.enums.ShiftDutyType;
import com.smartincident.incidentbackend.enums.ShiftTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkCheckpointShiftDto {
    private java.util.List<String> officerUids;
    private String checkpointUid;
    private LocalDate shiftDate;
    private ShiftTime shiftTime;
    private ShiftDutyType shiftDutyType;
    private LocalTime startTime;
    private LocalTime endTime;
}
