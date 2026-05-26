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
    private String uid;                    // null = create, non-null = update

    private String vehicleUid;
    private LocalDate shiftDate;
    private ShiftTime shiftTime;
    private LocalTime startTime;
    private LocalTime endTime;
    private String dutyDescription;

    // ── Police / fire crew ────────────────────────────────────────────────
    private List<String> crewUids;         // PoliceOfficer UIDs
    private String standbyLocationUid;     // TrafficCheckpoint UID (optional)

    // ── Ambulance crew ────────────────────────────────────────────────────
    /** UIDs of Medic entities assigned to this ambulance shift. */
    private List<String> medicUids;

    /** UID of the medic who is in charge (responsible, location-tracked). */
    private String inChargeUid;

    /** UID of the medic who will drive the ambulance. */
    private String driverUid;

    /** UID of the Hospital where the ambulance will be stationed. */
    private String standbyHospitalUid;

    /** Free-text standby / response point description. */
    private String standbyLocationName;
}
