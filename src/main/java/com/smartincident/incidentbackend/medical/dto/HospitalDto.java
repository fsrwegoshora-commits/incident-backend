package com.smartincident.incidentbackend.medical.dto;

import com.smartincident.incidentbackend.enums.HospitalType;
import com.smartincident.incidentbackend.police.dto.LocationDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HospitalDto {

    private String uid;             // null = create, non-null = update

    private String name;

    private HospitalType hospitalType;

    private LocationDto location;

    private Integer bedCapacity;

    /** Direct ambulance / emergency contact number. */
    private String emergencyContact;

    /**
     * UID of the EmergencyUnit (HOSPITAL_AMBULANCE_UNIT) linked to this hospital.
     * Set this after creating the ambulance unit via /api/emergency/units.
     */
    private String ambulanceUnitUid;
}
