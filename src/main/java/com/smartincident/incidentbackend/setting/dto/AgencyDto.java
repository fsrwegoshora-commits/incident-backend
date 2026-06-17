package com.smartincident.incidentbackend.setting.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgencyDto {
    private String uid;
    private String name;
    /** Legacy field — same as agencyType. */
    private String code;
    /** Preferred field sent by the frontend (POLICE, FIRE, MEDICAL, ECA). */
    private String agencyType;
    private String description;
    /** Optional — stored for display; not a database column on Agency. */
    private String contactPhone;
    private String address;

    /** Returns the effective agency type/code from whichever field is present. */
    public String getEffectiveCode() {
        return code != null && !code.isBlank() ? code
             : agencyType != null && !agencyType.isBlank() ? agencyType
             : null;
    }
}
