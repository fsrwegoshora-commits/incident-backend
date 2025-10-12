package com.smartincident.incidentbackend.police.dto;

import com.smartincident.incidentbackend.enums.ShiftType;
import com.smartincident.incidentbackend.police.entity.OfficerShift;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OfficerShiftDto {
    private String uid;
    private String officerUid;
    private LocalDate shiftDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private ShiftType shiftType;
    private String dutyDescription;
    private Boolean isExcused;
    private String excuseReason;
    private Boolean isPunishmentMode;
    private Boolean isReassigned;
    private String reassignedFromUid;

    public OfficerShiftDto(OfficerShift shift) {
        this.uid = shift.getUid();
        this.officerUid = shift.getOfficer().getUid();
        this.shiftDate = shift.getShiftDate();
        this.startTime = shift.getStartTime();
        this.endTime = shift.getEndTime();
        this.shiftType = shift.getShiftType();
        this.dutyDescription = shift.getDutyDescription();
        this.isExcused = shift.getIsExcused();
        this.excuseReason = shift.getExcuseReason();
        this.isPunishmentMode = shift.getIsPunishmentMode();
        this.isReassigned = shift.getIsReassigned();
        this.reassignedFromUid = shift.getReassignedFromUid();
    }
}


