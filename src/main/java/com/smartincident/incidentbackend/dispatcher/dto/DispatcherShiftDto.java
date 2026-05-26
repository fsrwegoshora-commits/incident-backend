package com.smartincident.incidentbackend.dispatcher.dto;

import com.smartincident.incidentbackend.enums.ShiftTime;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
public class DispatcherShiftDto {
    private String uid;
    private String dispatcherUid;
    private LocalDate shiftDate;
    private ShiftTime shiftTime;
    private LocalTime startTime;
    private LocalTime endTime;
    private String notes;
}
