package com.smartincident.incidentbackend.police.dto;

import com.smartincident.incidentbackend.enums.ShiftDutyType;
import com.smartincident.incidentbackend.enums.ShiftTime;
import com.smartincident.incidentbackend.enums.ShiftType;
import com.smartincident.incidentbackend.police.entity.OfficerShift;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfficerShiftDto {
    private String uid;
    private String officerUid;
    private LocalDate shiftDate;
    private ShiftTime shiftTime;
    private ShiftDutyType shiftDutyType;
    private LocalTime startTime;
    private LocalTime endTime;
    private String dutyDescription;
    private Boolean isPunishmentMode;
    private Boolean isExcused;
    private String excuseReason;
    private Boolean isReassigned;
    private String checkpointUid;

public OfficerShiftDto(OfficerShift shift) {
        this.uid = shift.getUid();
        this.officerUid = shift.getOfficer().getUid();
        this.shiftDate = shift.getShiftDate();
        this.startTime = shift.getStartTime();
        this.endTime = shift.getEndTime();
        this.shiftTime = shift.getShiftTime();
        this.shiftDutyType = shift.getShiftDutyType();
        this.dutyDescription = shift.getDutyDescription();
        this.isExcused = shift.getIsExcused();
        this.excuseReason = shift.getExcuseReason();
        this.isPunishmentMode = shift.getIsPunishmentMode();
        this.isReassigned = shift.getIsReassigned();
        this.checkpointUid = shift.getCheckpoint() != null ? shift.getCheckpoint().getUid() : null;
    }
}


