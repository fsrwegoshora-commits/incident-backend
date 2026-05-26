package com.smartincident.incidentbackend.emergency.dto;

import com.smartincident.incidentbackend.enums.UnitLevel;
import com.smartincident.incidentbackend.enums.UnitType;
import com.smartincident.incidentbackend.police.dto.LocationDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyUnitDto {

    private String uid;             // null = create, non-null = update

    private String name;

    /** Agency UID (FIRE or MEDICAL — police use PoliceStation). */
    private String agencyUid;

    /**
     * Unit type. Must be a fire or medical type.
     * FIRE: FIRE_BRIGADE, FIRE_DISTRICT_STATION, FIRE_REGIONAL_COMMAND, FIRE_NATIONAL_HQ, AIRPORT_FIRE_UNIT
     * MEDICAL: HOSPITAL_AMBULANCE_UNIT
     */
    private UnitType unitType;

    private UnitLevel level;

    /** UID of the parent unit in the chain of command. Null for top-level HQ. */
    private String parentUnitUid;

    private LocationDto location;

    /** UID of the AdministrativeArea this unit belongs to. */
    private String administrativeAreaUid;

    private String contactInfo;
}
