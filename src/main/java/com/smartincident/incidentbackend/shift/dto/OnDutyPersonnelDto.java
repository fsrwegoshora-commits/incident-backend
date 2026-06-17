package com.smartincident.incidentbackend.shift.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnDutyPersonnelDto {

    private int totalOnDuty;
    private List<PersonnelRecord> dispatchers;
    private List<PersonnelRecord> policeOfficers;
    private List<PersonnelRecord> fireOfficers;
    private List<PersonnelRecord> medics;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonnelRecord {
        private String uid;
        private String name;
        private String phoneNumber;
        private String role;
        private String unit;
        private String station;
        private String badgeOrServiceNumber;
        private LocalDate shiftDate;
        private LocalTime shiftStart;
        private LocalTime shiftEnd;
    }
}
