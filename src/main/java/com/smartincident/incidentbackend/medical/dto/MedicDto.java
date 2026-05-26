package com.smartincident.incidentbackend.medical.dto;

import com.smartincident.incidentbackend.enums.MedicRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicDto {

    private String uid;             // null = create, non-null = update

    /** Hospital-issued staff / employee number. */
    private String staffNumber;

    private MedicRole role;

    /** UID of the hospital this medic works for. */
    private String hospitalUid;

    /**
     * UID of the EmergencyUnit (HOSPITAL_AMBULANCE_UNIT) for dispatch.
     * Should match hospital.ambulanceUnit.
     */
    private String emergencyUnitUid;

    /** Link to an existing user account. If null, create one from name + phone. */
    private String userUid;

    // Inline user creation
    private String name;
    private String phoneNumber;

    private Boolean isOnDuty;
}
