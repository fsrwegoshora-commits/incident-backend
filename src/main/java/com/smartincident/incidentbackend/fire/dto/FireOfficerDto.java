package com.smartincident.incidentbackend.fire.dto;

import com.smartincident.incidentbackend.enums.FireRank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FireOfficerDto {

    private String uid;             // null = create, non-null = update

    /** TFRF service number (unique per officer). */
    private String serviceNumber;

    private FireRank rank;

    /** UID of the EmergencyUnit (fire type) this officer is assigned to. */
    private String emergencyUnitUid;

    /** Link to an existing user account. If null, create one from name + phone. */
    private String userUid;

    // Inline user creation (used when userUid is null)
    private String name;
    private String phoneNumber;

    private Boolean isOnDuty;
}
