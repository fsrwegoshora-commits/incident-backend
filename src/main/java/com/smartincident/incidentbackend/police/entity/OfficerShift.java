package com.smartincident.incidentbackend.police.entity;

import com.smartincident.incidentbackend.entity.BaseEntity;
import com.smartincident.incidentbackend.enums.ShiftType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "officer_shifts")
public class OfficerShift extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "officer_id", nullable = false)
    private PoliceOfficer officer;

    @Column(nullable = false)
    private LocalDate shiftDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShiftType shiftType;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    private String dutyDescription;

    private Boolean isExcused = false;

    private String excuseReason;

    @Column(nullable = false)
    private Boolean isPunishmentMode = false;

    private Boolean isReassigned = false;

    private String reassignedFromUid;

}

